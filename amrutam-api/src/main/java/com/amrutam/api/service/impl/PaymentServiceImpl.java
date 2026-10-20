package com.amrutam.api.service.impl;

import com.amrutam.api.audit.AuditRecorder;
import com.amrutam.api.dto.PaymentDto;
import com.amrutam.api.entity.Consultation;
import com.amrutam.api.entity.Doctor;
import com.amrutam.api.entity.Payment;
import com.amrutam.api.entity.PaymentStatus;
import com.amrutam.api.repository.DoctorRepository;
import com.amrutam.api.repository.PaymentRepository;
import com.amrutam.api.service.PaymentService;
import com.amrutam.common.exception.ConflictException;
import com.amrutam.common.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link PaymentService} implementation.
 *
 * <p>Gateway capture is simulated synchronously: {@link #capture(UUID)}
 * marks a PAYING payment as CAPTURED exactly once (the transition is
 * guarded by an atomic status update so two captures can never both win);
 * calling it again on an already-CAPTURED payment is a no-op. A real
 * gateway would be async; compensation paths in the booking saga mark the
 * payment FAILED when capture fails.</p>
 *
 * <p>The charge amount is resolved from the consultation's doctor's
 * consultation fee ({@code 0.00} when the fee is unset); the previous
 * 500.00 placeholder is gone.</p>
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository payments;
    private final DoctorRepository doctors;
    private final AuditRecorder audit;

    /**
     * @param payments payment repository
     * @param doctors doctor repository (fee lookup)
     * @param audit durable audit recorder
     */
    public PaymentServiceImpl(PaymentRepository payments,
                              DoctorRepository doctors,
                              AuditRecorder audit) {
        this.payments = payments;
        this.doctors = doctors;
        this.audit = audit;
    }

    @Override
    @Transactional
    public PaymentDto createPayment(Consultation consultation, String idempotencyKey, String paymentToken) {
        // A blank key means "generate one": the payment's own row is the
        // idempotency identity.
        String key = (idempotencyKey == null || idempotencyKey.isBlank())
                ? UUID.randomUUID().toString()
                : idempotencyKey;
        Optional<Payment> existing = payments.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            log.debug("Idempotent payment replay key={} paymentId={}", key, existing.get().getId());
            return toDto(existing.get());
        }

        BigDecimal amount = resolveAmount(consultation);
        Payment payment = new Payment();
        payment.setConsultationId(consultation.getId());
        payment.setAmount(amount);
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.PAYING);
        payment.setGatewayRef(paymentToken);
        payment.setIdempotencyKey(key);
        Instant now = Instant.now();
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        Payment saved = payments.save(payment);
        audit.record(consultation.getPatientId(), "payment.create", "payment:" + saved.getId(), null,
                Map.of("consultationId", consultation.getId().toString(), "amount", amount.toString()));
        log.info("Created payment id={} consultationId={} amount={} key={}",
                saved.getId(), saved.getConsultationId(), saved.getAmount(), key);
        return toDto(saved);
    }

    @Override
    @Transactional
    public PaymentDto capture(UUID paymentId) {
        int updated = payments.captureIfPaying(paymentId);
        if (updated == 0) {
            Payment p = getEntity(paymentId);
            // Idempotent: already settled.
            if (p.getStatus() == PaymentStatus.CAPTURED) {
                log.debug("capture({}) was a no-op; already CAPTURED", paymentId);
                return toDto(p);
            }
            throw new ConflictException("Payment is not in PAYING state: " + p.getStatus());
        }
        Payment p = getEntity(paymentId);
        audit.record(null, "payment.capture", "payment:" + paymentId, null,
                Map.of("consultationId", p.getConsultationId().toString()));
        log.info("Captured payment id={}", paymentId);
        return toDto(p);
    }

    @Override
    @Transactional
    public PaymentDto refund(UUID paymentId) {
        int updated = payments.refundIfCaptured(paymentId);
        if (updated == 0) {
            Payment p = getEntity(paymentId);
            // Idempotent: already refunded.
            if (p.getStatus() == PaymentStatus.REFUNDED) {
                log.debug("refund({}) was a no-op; already REFUNDED", paymentId);
                return toDto(p);
            }
            throw new ConflictException("Payment is not in CAPTURED state: " + p.getStatus());
        }
        Payment p = getEntity(paymentId);
        audit.record(null, "payment.refund", "payment:" + paymentId, null,
                Map.of("consultationId", p.getConsultationId().toString()));
        log.info("Refunded payment id={}", paymentId);
        return toDto(p);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDto byId(UUID paymentId) {
        return toDto(getEntity(paymentId));
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getEntity(UUID paymentId) {
        return payments.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("payment", String.valueOf(paymentId)));
    }

    /**
     * The consultation's doctor's fee (0.00 when the doctor row or fee is
     * missing).
     */
    private BigDecimal resolveAmount(Consultation consultation) {
        Doctor doctor = doctors.findById(consultation.getDoctorId()).orElse(null);
        if (doctor == null || doctor.getConsultationFee() == null) {
            return BigDecimal.ZERO;
        }
        return doctor.getConsultationFee();
    }

    private PaymentDto toDto(Payment p) {
        return new PaymentDto(p.getId(), p.getConsultationId(), p.getAmount(), p.getCurrency(),
                p.getStatus() == null ? null : p.getStatus().name(), p.getGatewayRef());
    }
}
