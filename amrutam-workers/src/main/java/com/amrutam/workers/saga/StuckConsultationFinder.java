package com.amrutam.workers.saga;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Reads consultations stuck in {@code PENDING_PAYMENT} past the capture
 * deadline.
 *
 * <p>Owns no entity: SELECTs against the shared {@code consultations} table
 * with parameterized JDBC. The api module's {@code consultations} schema is
 * the source of truth for column names — if it adds a {@code status} column
 * rename, update this query in lockstep.</p>
 *
 * @param id          consultation id
 * @param createdAt  when the consultation was created (drives the stuck age)
 */
record StuckConsultation(UUID id, Instant createdAt) {
}

// NOTE: the record is declared in this file (not its own file) so the finder
// class and the record stay in the same file — H2/Postgres both see it fine,
// and it keeps the test classpath simpler.

/**
 * Finds consultations whose payment-capture deadline has passed.
 */
@Component
public class StuckConsultationFinder {

    private final JdbcTemplate jdbc;

    /**
     * @param jdbc JdbcTemplate
     */
    public StuckConsultationFinder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @param stuckAfter age threshold — consultations older than this and
     *                   still PENDING_PAYMENT are considered stuck
     * @param limit      max rows per sweep
     * @return stuck consultations, oldest first
     */
    public List<StuckConsultation> findStuck(Duration stuckAfter, int limit) {
        Instant cutoff = Instant.now().minus(stuckAfter);
        return jdbc.query(
                "SELECT id, created_at FROM consultations "
                        + "WHERE status = 'PENDING_PAYMENT' AND created_at < ? "
                        + "ORDER BY created_at LIMIT ?",
                (rs, n) -> new StuckConsultation(
                        UUID.fromString(rs.getString("id")),
                        rs.getTimestamp("created_at").toInstant()),
                java.sql.Timestamp.from(cutoff), limit);
    }
}
