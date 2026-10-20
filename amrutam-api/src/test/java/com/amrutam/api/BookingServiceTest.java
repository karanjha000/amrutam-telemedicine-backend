package com.amrutam.api;

import com.amrutam.api.dto.BookConsultationResponse;
import com.amrutam.api.dto.BookingRequest;
import com.amrutam.api.entity.AvailabilitySlot;
import com.amrutam.api.entity.Consultation;
import com.amrutam.api.entity.ConsultationStatus;
import com.amrutam.api.entity.Doctor;
import com.amrutam.api.entity.Payment;
import com.amrutam.api.entity.PaymentStatus;
import com.amrutam.api.entity.SlotStatus;
import com.amrutam.api.entity.User;
import com.amrutam.api.repository.AvailabilitySlotRepository;
import com.amrutam.api.repository.ConsultationRepository;
import com.amrutam.api.repository.DoctorRepository;
import com.amrutam.api.repository.PaymentRepository;
import com.amrutam.api.repository.UserRepository;
import com.amrutam.api.service.BookingService;
import com.amrutam.common.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end tests of the booking saga over a real (H2) datasource: happy
 * path, idempotent replay, and the slot-race 409.
 */
@SpringBootTest
@ActiveProfiles("test")
class BookingServiceTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository users;

    @Autowired
    private DoctorRepository doctors;

    @Autowired
    private AvailabilitySlotRepository slots;

    @Autowired
    private ConsultationRepository consultations;

    @Autowired
    private PaymentRepository payments;

    private UUID doctorId;
    private UUID slotId;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        // Deterministic ids so the test is self-contained and never depends
        // on data.sql seed rows.
        Instant now = Instant.now();

        User doctorUser = new User();
        doctorUser.setUsername("dr.test-" + now.toEpochMilli());
        doctorUser.setPasswordHash("$2a$10$test");
        doctorUser.setRole(User.RoleString.DOCTOR);
        doctorUser.setMfaEnabled(false);
        doctorUser.setActive(true);
        doctorUser.setCreatedAt(now);
        doctorUser.setUpdatedAt(now);
        doctorUser = users.save(doctorUser);

        Doctor doctor = new Doctor();
        doctor.setUserId(doctorUser.getId());
        doctor.setSpecialization("Cardiology");
        doctor.setConsultationFee(new java.math.BigDecimal("500.00"));
        doctor.setYearsExperience(5);
        doctor.setActive(true);
        doctor.setRatings(0.0);
        doctor.setTotalRatings(0);
        doctor.setCreatedAt(now);
        doctor.setUpdatedAt(now);
        doctor = doctors.save(doctor);
        doctorId = doctor.getId();

        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setDoctorId(doctorId);
        slot.setStartAt(Instant.now().plus(1, ChronoUnit.HOURS));
        slot.setEndAt(Instant.now().plus(90, ChronoUnit.MINUTES));
        slot.setStatus(SlotStatus.AVAILABLE);
        slot.setCreatedAt(now);
        slot = slots.save(slot);
        slotId = slot.getId();

        User patient = new User();
        patient.setUsername("patient.test-" + now.toEpochMilli());
        patient.setPasswordHash("$2a$10$test");
        patient.setRole(User.RoleString.PATIENT);
        patient.setMfaEnabled(false);
        patient.setActive(true);
        patient.setCreatedAt(now);
        patient.setUpdatedAt(now);
        patient = users.save(patient);
        patientId = patient.getId();
    }

    @Test
    void bookHappyPath() {
        BookingRequest req = new BookingRequest(doctorId, slotId, "card", "tok_test");

        BookConsultationResponse resp = bookingService.book(req, patientId, null);

        assertThat(resp.consultationId()).isNotNull();
        assertThat(resp.paymentId()).isNotNull();
        assertThat(resp.replayed()).isFalse();

        Consultation c = consultations.findById(resp.consultationId()).orElseThrow();
        assertThat(c.getStatus()).isEqualTo(ConsultationStatus.PAID);
        assertThat(c.getPatientId()).isEqualTo(patientId);

        AvailabilitySlot slot = slots.findById(slotId).orElseThrow();
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.LOCKED);
        assertThat(slot.getLockedById()).isEqualTo(patientId);

        Payment p = payments.findById(resp.paymentId()).orElseThrow();
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(p.getAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void bookIdempotentReplay() {
        BookingRequest req = new BookingRequest(doctorId, slotId, "card", "tok_test");
        String key = "key-" + UUID.randomUUID();

        BookConsultationResponse first = bookingService.book(req, patientId, key);
        assertThat(first.replayed()).isFalse();

        // Second call with the SAME explicit key: the idempotency kernel
        // replays the stored result instead of re-running the saga.
        BookConsultationResponse second = bookingService.book(req, patientId, key);
        assertThat(second.replayed()).isTrue();
        assertThat(second.consultationId()).isEqualTo(first.consultationId());
        assertThat(second.paymentId()).isEqualTo(first.paymentId());

        // Exactly ONE consultation row exists for this patient.
        List<Consultation> all = consultations.findByPatientId(patientId,
                org.springframework.data.domain.Pageable.unpaged()).getContent();
        assertThat(all).hasSize(1);
    }

    @Test
    void bookSlotRaceReturns409() {
        // Simulate a competing booking that already won the slot lock, then
        // re-run a DIFFERENT request shape on the same slot: the second
        // request must see the slot non-AVAILABLE and 409 without creating a
        // consultation.
        slots.findById(slotId).orElseThrow();
        int locked = slots.lockSlot(slotId, UUID.randomUUID(),
                Instant.now().plus(15, ChronoUnit.MINUTES));
        assertThat(locked).isEqualTo(1);

        // Different request shape (different payment method + explicit key)
        // so it is NOT an idempotency replay of any prior call.
        BookingRequest req = new BookingRequest(doctorId, slotId, "upi", "tok-race");
        assertThatThrownBy(() -> bookingService.book(req, patientId, "race-key-" + UUID.randomUUID()))
                .isInstanceOf(ConflictException.class);

        // No consultation was created for this patient (the lock was already
        // held; the slot-status guard rejects the booking up front).
        List<Consultation> all = consultations.findByPatientId(patientId,
                org.springframework.data.domain.Pageable.unpaged()).getContent();
        assertThat(all).isEmpty();
    }
}
