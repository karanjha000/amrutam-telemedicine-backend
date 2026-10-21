package com.amrutam.workers.saga;

// import java.sql.Timestamp;
// import java.time.Instant;
// import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The compensation half of the booking saga.
 *
 * <p>Each step is a single parameterized UPDATE run inside a
 * {@link TransactionTemplate} so the three steps (fail the payment, release
 * the slot lock, cancel the consultation) are atomic. The steps are ordered
 * so that whichever one throws first still leaves the database in a
 * consistent, re-entrant state for a later sweep — e.g. if the slot UPDATE
 * throws, the payment is already marked FAILED and the next sweep simply
 * re-runs from that point.</p>
 *
 * <p>Boundary: raw JdbcTemplate, matching the column set of the api module's
 * {@code schema.sql} for {@code payments}, {@code consultations}, and
 * {@code availability_slots} (see {@code docs/ARCHITECTURE.md}).</p>
 */
@Component
public class CompensationExecutor {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    /**
     * @param jdbc shared-datasource JdbcTemplate
     * @param tx   Spring transaction template (from the shared DataSource)
     */
    public CompensationExecutor(JdbcTemplate jdbc, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.tx = tx;
    }

    /**
     * Runs the full compensation sequence for one stuck consultation in a
     * single DB transaction.
     *
     * @param consultationId the consultation to compensate
     * @return {@code true} if the compensation ran, {@code false} if the
     *         consultation was already CANCELLED (no-op)
     */
    public boolean compensate(UUID consultationId) {
        return Boolean.TRUE.equals(tx.execute(status -> {
            markUncapturedPaymentsFailed(consultationId);
            releaseLockedSlot(consultationId);
            int updated = cancelConsultation(consultationId);
            return updated > 0;
        }));
    }

    /**
     * Fails any payment for this consultation that has not yet been captured.
     * Rows already CAPTURED are deliberately untouched — a captured payment
     * means the user was charged; compensation must not silently delete that
     * fact.
     *
     * @param consultationId the consultation owning the payments
     */
    public void markUncapturedPaymentsFailed(UUID consultationId) {
        jdbc.update(
                "UPDATE payments SET status = 'FAILED', updated_at = now() "
                        + "WHERE consultation_id = ? AND status IN ('CREATED', 'PAYING')",
                consultationId);
    }

    /**
     * Releases the slot lock held for this consultation (only when still
     * LOCKED — a naturally-expired or already-released lock is a no-op).
     *
     * @param consultationId the consultation holding the lock
     */
    public void releaseLockedSlot(UUID consultationId) {
        jdbc.update(
                "UPDATE availability_slots SET status = 'AVAILABLE', "
                        + "locked_by_id = NULL, lock_expires_at = NULL, updated_at = now() "
                        + "WHERE id = (SELECT slot_id FROM consultations WHERE id = ?) "
                        + "AND status = 'LOCKED'",
                consultationId);
    }

    /**
     * Cancels the consultation itself.
     *
     * @param consultationId the consultation to cancel
     * @return rows updated (0 if already cancelled)
     */
    public int cancelConsultation(UUID consultationId) {
        return jdbc.update(
                "UPDATE consultations SET status = 'CANCELLED', updated_at = now() "
                        + "WHERE id = ? AND status = 'PENDING_PAYMENT'",
                consultationId);
    }
}
