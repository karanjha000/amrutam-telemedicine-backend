package com.amrutam.workers.saga;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
// import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.amrutam.workers.config.WorkersProperties;

/**
 * Booking-saga RECONCILER.
 *
 * <p><b>Design decision (reconciliation saga vs. forward-driven saga).</b> The
 * forward path of the booking saga — create-consultation, lock-slot,
 * create-payment, confirm-payment — runs synchronously, in a single DB
 * transaction, inside amrutam-api's {@code BookingService}. Its compensation
 * (unlock-slot, cancel-consultation, refund-payment) is likewise
 * transaction-local, driven by that service's {@code catch} blocks. What that
 * leaves unhandled is the window between "payment created, PENDING_PAYMENT"
 * and "payment captured": if the capture call is lost (gateway timeout,
 * worker crash, payment-provider downtime) nobody in-process will ever come
 * back and finish the saga.</p>
 *
 * <p>This orchestrator closes that window as a <i>separate process</i>: it
 * periodically finds consultations that have been stuck in
 * {@code PENDING_PAYMENT} for longer than {@code sagaStuckAfterMinutes}
 * (the payment-capture deadline) and runs the compensation half — fail any
 * uncaptured payment, release the slot lock, cancel the consultation — as a
 * persisted-state reconciler. It does <b>not</b> re-run the forward path;
 * it only walks the compensation path to a consistent terminal state.</p>
 *
 * <p>See {@code docs/ARCHITECTURE.md} § booking-saga for the full diagram
 * (forward + compensation + this reconciler as the backstop).</p>
 */
@Component
@ConditionalOnProperty(name = "amrutam.workers.enabled", havingValue = "true", matchIfMissing = true)
public class BookingSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(BookingSagaOrchestrator.class);
    private static final int BATCH = 50;

    private final StuckConsultationFinder finder;
    private final CompensationExecutor compensation;
    private final WorkersProperties props;

    private final java.util.concurrent.atomic.AtomicReference<Instant> lastReconcileAt =
            new java.util.concurrent.atomic.AtomicReference<>(Instant.EPOCH);

    /**
     * @param finder       stuck-consultation query
     * @param compensation compensation (compensating transaction)
     * @param props        workers configuration
     */
    public BookingSagaOrchestrator(StuckConsultationFinder finder,
                                   CompensationExecutor compensation,
                                   WorkersProperties props) {
        this.finder = finder;
        this.compensation = compensation;
        this.props = props;
    }

    /**
     * One reconciliation sweep. Also public so tests can drive it manually
     * instead of waiting for the scheduler.
     */
    @Scheduled(fixedDelayString = "${amrutam.workers.saga-reconciliation-interval-ms:30000}")
    public void reconcile() {
        lastReconcileAt.set(Instant.now());
        List<StuckConsultation> stuck = finder.findStuck(
                Duration.ofMinutes(props.getSagaStuckAfterMinutes()), BATCH);
        for (StuckConsultation c : stuck) {
            // Per-item isolation: one bad row must not poison the batch.
            try {
                boolean acted = compensation.compensate(c.id());
                log.info("Saga compensation for consultation {} (created {}) -> {}",
                        c.id(), c.createdAt(), acted ? "CANCELLED" : "already CANCELLED (no-op)");
            } catch (Exception e) {
                log.error("Saga compensation for consultation {} failed, will retry next sweep: {}",
                        c.id(), e.getMessage(), e);
            }
        }
    }

    /**
     * @return last sweep start time (for observability)
     */
    public Instant lastReconcileAt() {
        return lastReconcileAt.get();
    }
}
