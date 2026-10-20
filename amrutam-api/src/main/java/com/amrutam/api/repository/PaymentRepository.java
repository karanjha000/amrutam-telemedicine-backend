package com.amrutam.api.repository;

import com.amrutam.api.entity.Payment;
import com.amrutam.api.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Payment}.
 */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * @param idempotencyKey idempotency key
     * @return the payment, if present
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * @param consultationId consultation
     * @return the payment, if present
     */
    Optional<Payment> findByConsultationId(UUID consultationId);

    /**
     * @param status status
     * @param pageable paging
     * @return matching page
     */
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    /**
     * Count of payments in a status + created-window (revenue analytics).
     *
     * @param st status
     * @param f window start
     * @param t window end
     * @return count
     */
    long countByStatusAndCreatedAtBetween(PaymentStatus st, Instant f, Instant t);

    /**
     * Sum of captured amounts within a window (revenue analytics).
     *
     * @param from window start
     * @param to window end
     * @return the sum, or empty if no captured payments in the window
     */
    @Query("select sum(p.amount) from Payment p where p.status = 'CAPTURED' "
            + "and p.createdAt between :from and :to")
    Optional<BigDecimal> sumCapturedBetween(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * Transition {@code PAYING -> CAPTURED} (exactly one winner).
     *
     * @param id payment id
     * @param now timestamp for {@code updated_at}
     * @return rows affected
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("update Payment p set p.status = 'CAPTURED', p.updatedAt = :now "
            + "where p.id = :id and p.status = 'PAYING'")
    int captureIfPaying(@Param("id") UUID id, @Param("now") Instant now);

    /**
     * Transition {@code CAPTURED -> REFUNDED}.
     *
     * @param id payment id
     * @param now timestamp for {@code updated_at}
     * @return rows affected
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("update Payment p set p.status = 'REFUNDED', p.updatedAt = :now "
            + "where p.id = :id and p.status = 'CAPTURED'")
    int refundIfCaptured(@Param("id") UUID id, @Param("now") Instant now);

    /**
     * Transition to FAILED.
     *
     * @param id payment id
     * @param now timestamp for {@code updated_at}
     * @return rows affected
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("update Payment p set p.status = 'FAILED', p.updatedAt = :now where p.id = :id")
    int markFailed(@Param("id") UUID id, @Param("now") Instant now);
}
