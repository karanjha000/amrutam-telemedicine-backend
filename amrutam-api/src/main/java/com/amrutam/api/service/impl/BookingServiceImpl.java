package com.amrutam.api.service.impl;

import com.amrutam.api.dto.BookConsultationResponse;
import com.amrutam.api.dto.BookingRequest;
import com.amrutam.api.dto.ConsultationDto;
import com.amrutam.api.dto.PaymentDto;
import com.amrutam.api.entity.AvailabilitySlot;
import com.amrutam.api.entity.Consultation;
import com.amrutam.api.entity.ConsultationStatus;
import com.amrutam.api.entity.PaymentStatus;
import com.amrutam.api.entity.SlotStatus;
import com.amrutam.api.repository.AsyncJobRepository;
import com.amrutam.api.repository.AvailabilitySlotRepository;
import com.amrutam.api.repository.ConsultationRepository;
import com.amrutam.api.repository.DoctorRepository;
import com.amrutam.api.repository.PaymentRepository;
import com.amrutam.api.service.BookingService;
import com.amrutam.api.service.PaymentService;
import com.amrutam.api.workerbridge.AsyncBridge;
import com.amrutam.common.exception.BadRequestException;
import com.amrutam.common.exception.ConflictException;
import com.amrutam.common.exception.ForbiddenException;
import com.amrutam.common.exception.NotFoundException;
import com.amrutam.common.idempotency.IdempotencyOutcome;
import com.amrutam.common.idempotency.IdempotencyResult;
import com.amrutam.common.idempotency.IdempotencyService;
import com.amrutam.common.idempotency.IdempotencySupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
// import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@link BookingService} implementation — the booking saga.
 *
 * <p>Steps (numbered to mirror the saga trace written via
 * {@link AsyncBridge#recordSagaStep}):</p>
 * <ol>
 *   <li>load the slot + doctor (404 when either is unknown; 400 when the slot
 *       does not belong to the requested doctor; 409 when the slot is not
 *       AVAILABLE);</li>
 *   <li>insert the consultation in PENDING_PAYMENT;</li>
 *   <li>atomically lock the slot ({@code AVAILABLE -> LOCKED}); the guarded
 *       {@code UPDATE ... WHERE status = 'AVAILABLE'} is the source of truth
 *       against a competing booking — 409 when a rival won the row between
 *       the read and the lock;</li>
 *   <li>create the payment (PAYING) via {@link PaymentService} (amount = the
 *       doctor's consultation fee);</li>
 *   <li>capture the payment (PAYING -&gt; CAPTURED); any failure compensates:
 *       payment FAILED, slot lock released (LOCKED -&gt; CANCELLED),
 *       consultation CANCELLED, then the original exception is rethrown;</li>
 *   <li>on success mark the consultation PAID and enqueue the
 *       scheduled-notification async job.</li>
 * </ol>
 *
 * <p><b>Idempotency:</b> steps 2-6 run inside {@link IdempotencyService#apply}
 * with key {@code (patientId, POST, /api/v1/consultations/booking, bodyHash)}.
 * When the {@code X-Idempotency-Key} header is present it <i>replaces</i> the
 * body hash as the identity (explicit keys win: duplicate requests with the
 * same key replay regardless of body changes); otherwise the identity is the
 * salted SHA-256 of the canonicalized request body. A failing booking leaves
 * the row PENDING; it expires after the kernel's 5-minute window and the
 * client may then retry.</p>
 */
@Service
public class BookingServiceImpl implements BookingService {

    /** Lock window for a newly LOCKED slot. */
    public static final Duration LOCK_WINDOW = Duration.ofMinutes(15);

    /** Idempotency namespace for this saga. */
    private static final String BOOKING_METHOD = "POST";
    private static final String BOOKING_PATH = "/api/v1/consultations/booking";

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final AvailabilitySlotRepository slots;
    private final ConsultationRepository consultations;
    private final DoctorRepository doctorRepo;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepo;
    private final AsyncBridge asyncBridge;
    private final IdempotencyService idempotency;
    private final ObjectMapper json;
    private final String salt;

    /**
     * @param slots availability repository
     * @param consultations consultation repository
     * @param doctorRepo doctor repository
     * @param paymentService payment service
     * @param paymentRepo payment repository
     * @param asyncBridge async work seam
     * @param idempotency idempotency service (common kernel)
     * @param json json mapper
     * @param salt hash salt for the booking idempotency key
     */
    public BookingServiceImpl(AvailabilitySlotRepository slots,
                              ConsultationRepository consultations,
                              DoctorRepository doctorRepo,
                              PaymentService paymentService,
                              PaymentRepository paymentRepo,
                              AsyncBridge asyncBridge,
                              IdempotencyService idempotency,
                              ObjectMapper json,
                              @Value("${amrutam.idempotency.salt:amrutam-booking}") String salt) {
        this.slots = slots;
        this.consultations = consultations;
        this.doctorRepo = doctorRepo;
        this.paymentService = paymentService;
        this.paymentRepo = paymentRepo;
        this.asyncBridge = asyncBridge;
        this.idempotency = idempotency;
        this.json = json;
        this.salt = salt;
    }

    @Override
    @Transactional
    public BookConsultationResponse book(BookingRequest request, UUID patientId, String explicitKey) {
        // An explicit X-Idempotency-Key REPLACES the body hash as the
        // idempotency identity: same key + different body still replays.
        String bodyHash;
        if (explicitKey != null && !explicitKey.isBlank()) {
            bodyHash = IdempotencySupport.bodyHash(explicitKey, patientId.toString());
        } else {
            bodyHash = IdempotencySupport.bodyHash(toCanonicalJson(request), salt);
        }

        IdempotencyOutcome outcome = idempotency.apply(
                patientId, BOOKING_METHOD, BOOKING_PATH, bodyHash,
                () -> IdempotencyResult.of(doBook(request, patientId)));
        if (outcome.replayed()) {
            return fromReplay(outcome.replayedPayload());
        }
        return (BookConsultationResponse) outcome.result().payload();
    }

    private BookConsultationResponse doBook(BookingRequest request, UUID patientId) {
        // 1) Load the slot + doctor, verify the slot belongs to the doctor.
        AvailabilitySlot slot = slots.findById(request.slotId())
                .orElseThrow(() -> new NotFoundException("slot", String.valueOf(request.slotId())));
        if (!doctorRepo.existsById(request.doctorId())) {
            throw new NotFoundException("doctor", String.valueOf(request.doctorId()));
        }
        if (!slot.getDoctorId().equals(request.doctorId())) {
            throw new BadRequestException("Slot does not belong to the given doctor");
        }
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new ConflictException("Slot is not available: " + slot.getStatus());
        }

        // 2) Create the consultation in PENDING_PAYMENT.
        Consultation consultation = new Consultation();
        consultation.setPatientId(patientId);
        consultation.setDoctorId(request.doctorId());
        consultation.setSlotId(slot.getId());
        consultation.setStatus(ConsultationStatus.PENDING_PAYMENT);
        consultation.setScheduledAt(slot.getStartAt());
        Instant now = Instant.now();
        consultation.setCreatedAt(now);
        consultation.setUpdatedAt(now);
        consultation = consultations.save(consultation);
        UUID sagaId = consultation.getId();
        asyncBridge.recordSagaStep(sagaId, 0, "create-consultation", "DONE",
                String.valueOf(sagaId));

        PaymentDto createdPayment = null;
        try {
            // 3) Atomically lock the slot (exactly one winner).
            int locked = slots.lockSlot(slot.getId(), patientId, now.plus(LOCK_WINDOW));
            if (locked == 0) {
                throw new ConflictException("Slot was just taken by another user; please retry");
            }
            asyncBridge.recordSagaStep(sagaId, 1, "lock-slot", "DONE", String.valueOf(slot.getId()));

            // 4) Create the payment (PAYING; amount resolved from the
            // doctor's consultation fee inside the payment service).
            createdPayment = paymentService.createPayment(consultation, null, request.paymentToken());
            asyncBridge.recordSagaStep(sagaId, 2, "create-payment", "DONE",
                    String.valueOf(createdPayment.id()));

            // 5) Capture; any failure here lands in the catch block, which
            // compensates the whole saga before rethrowing.
            PaymentDto captured = paymentService.capture(createdPayment.id());
            consultation.setStatus(ConsultationStatus.PAID);
            consultation.setUpdatedAt(Instant.now());
            consultations.save(consultation);
            asyncBridge.recordSagaStep(sagaId, 3, "capture-payment", "DONE",
                    String.valueOf(captured.id()));
        } catch (RuntimeException ex) {
            // Compensation: payment FAILED, slot released, consultation
            // CANCELLED, saga step traced, then rethrow.
            if (createdPayment != null) {
                paymentRepo.markFailed(createdPayment.id(), Instant.now());
            }
            slots.unlockSlot(slot.getId());
            consultation.setStatus(ConsultationStatus.CANCELLED);
            consultation.setUpdatedAt(Instant.now());
            consultations.save(consultation);
            asyncBridge.recordSagaStep(sagaId, 9, "compensation", "COMPENSATED",
                    ex.getMessage());
            log.info("Booking saga compensated consultation={} slot={}: {}",
                    sagaId, slot.getId(), ex.getMessage());
            throw ex;
        }

        // 6) Enqueue the scheduled-notification job.
        asyncBridge.enqueueAsyncJob(patientId,
                AsyncJobRepository.JobType.CONSULTATION_SCHEDULED_NOTIFICATION,
                Map.of("consultationId", consultation.getId().toString(),
                        "doctorId", request.doctorId().toString(),
                        "scheduledAt", slot.getStartAt().toString(),
                        "patientId", patientId.toString()));

        log.info("Booked consultation={} slot={} patient={} payment={}",
                consultation.getId(), slot.getId(), patientId, createdPayment.id());
        return new BookConsultationResponse(consultation.getId(), slot.getId(),
                createdPayment.id(), ConsultationStatus.PAID.name(), false);
    }

    /**
     * Reconstitutes the stored result after an idempotency replay.
     */
    private BookConsultationResponse fromReplay(String storedJson) {
        if (storedJson == null) {
            throw new ConflictException("idempotency replay had no stored result");
        }
        BookConsultationResponse stored;
        try {
            stored = json.readValue(storedJson, BookConsultationResponse.class);
        } catch (Exception e) {
            log.warn("Failed to rehydrate stored booking result: {}", e.getMessage());
            throw new ConflictException("Failed to replay booking result");
        }
        return new BookConsultationResponse(stored.consultationId(), stored.slotId(),
                stored.paymentId(), stored.status(), true);
    }

    private String toCanonicalJson(Object request) {
        try {
            return json.writeValueAsString(request);
        } catch (Exception e) {
            throw new BadRequestException("Failed to serialize booking request");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultationDto> myConsultations(UUID patientId, Pageable pageable) {
        return consultations.findByPatientId(patientId, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationDto get(UUID id) {
        Consultation c = consultations.findById(id)
                .orElseThrow(() -> new NotFoundException("consultation", String.valueOf(id)));
        return toDto(c);
    }

    @Override
    @Transactional
    public void confirmBooking(UUID consultationId) {
        // Future payment-webhook entry point; no state change yet.
        consultations.findById(consultationId)
                .orElseThrow(() -> new NotFoundException("consultation", String.valueOf(consultationId)));
        log.info("saga confirm (future payment-webhook entry point) consultation={}", consultationId);
    }

    @Override
    @Transactional
    public void cancel(UUID consultationId, UUID actorId) {
        Consultation c = consultations.findById(consultationId)
                .orElseThrow(() -> new NotFoundException("consultation", String.valueOf(consultationId)));
        if (!c.getPatientId().equals(actorId)) {
            throw new ForbiddenException("not the owner of this consultation");
        }
        if (c.getStatus() == ConsultationStatus.CANCELLED || c.getStatus() == ConsultationStatus.COMPLETED) {
            throw new ConflictException("consultation already " + c.getStatus());
        }
        // Release the slot lock; an uncaptured payment is marked FAILED. A
        // CAPTURED payment is left as-is for the future refund workflow.
        slots.unlockSlot(c.getSlotId());
        paymentRepo.findByConsultationId(consultationId)
                .ifPresent(p -> {
                    if (p.getStatus() == PaymentStatus.PAYING) {
                        paymentRepo.markFailed(p.getId(), Instant.now());
                    } else if (p.getStatus() == PaymentStatus.CAPTURED) {
                        log.info("Canceling consultation {} with a CAPTURED payment {} — "
                                + "refund workflow is a follow-up", consultationId, p.getId());
                    }
                });
        c.setStatus(ConsultationStatus.CANCELLED);
        c.setUpdatedAt(Instant.now());
        consultations.save(c);
        log.info("Cancelled consultation={} by user={}", consultationId, actorId);
    }

    @Override
    @Transactional
    public void complete(UUID consultationId) {
        Consultation c = consultations.findById(consultationId)
                .orElseThrow(() -> new NotFoundException("consultation", String.valueOf(consultationId)));
        if (c.getStatus() == ConsultationStatus.PENDING_PAYMENT) {
            // TODO: strict payment-webhook gating; tolerating completion of a
            // PENDING_PAYMENT consultation while the simulated capture is the
            // settlement path.
            log.warn("Completing consultation {} in PENDING_PAYMENT (payment webhook follow-up)",
                    consultationId);
        } else if (c.getStatus() != ConsultationStatus.PAID) {
            throw new ConflictException("consultation not completable from status " + c.getStatus());
        }
        c.setStatus(ConsultationStatus.COMPLETED);
        c.setCompletedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        consultations.save(c);
        log.info("Completed consultation={}", consultationId);
    }

    private ConsultationDto toDto(Consultation c) {
        return new ConsultationDto(c.getId(), c.getPatientId(), c.getDoctorId(), c.getSlotId(),
                c.getStatus() == null ? null : c.getStatus().name(), c.getScheduledAt(), c.getCompletedAt());
    }
}
