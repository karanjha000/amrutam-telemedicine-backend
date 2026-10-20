package com.amrutam.workers.saga;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Drives {@link BookingSagaOrchestrator#reconcile()} against the H2 test
 * datasource and asserts the three compensation effects (payment FAILED,
 * slot released, consultation CANCELLED) plus the fresh-consultation
 * negative case.
 *
 * <p>{@code @SpringBootTest} boots the real context under the {@code test}
 * profile so the auto-configured {@code TransactionTemplate} bean is
 * present. {@code reconcile()} is called manually — no scheduler wait.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource("classpath:application-test.yml")
class BookingSagaOrchestratorTest {

    @Autowired JdbcTemplate jdbc;

    @Autowired BookingSagaOrchestrator orchestrator;

    private UUID doctor;
    private UUID patient;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM payments");
        jdbc.update("DELETE FROM consultations");
        jdbc.update("DELETE FROM availability_slots");
        doctor = UUID.randomUUID();
        patient = UUID.randomUUID();
    }

    private UUID seedSlot(String status) {
        UUID slot = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO availability_slots (id, doctor_id, start_at, end_at, status, "
                        + "locked_by_id, lock_expires_at) VALUES (?, ?, now(), now() + 3600, ?, ?, ?)",
                slot, doctor, status,
                "AVAILABLE".equals(status) ? null : patient,
                "LOCKED".equals(status)
                        ? new Timestamp(Instant.now().plus(Duration.ofHours(1)).toEpochMilli())
                        : null);
        return slot;
    }

    private UUID seedConsultation(UUID slot, String status, int createdAtMinusMinutes) {
        UUID c = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO consultations (id, doctor_id, patient_id, slot_id, status, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                c, doctor, patient, slot, status,
                new Timestamp(Instant.now().minus(Duration.ofMinutes(createdAtMinusMinutes)).toEpochMilli()));
        return c;
    }

    private void seedPayment(UUID consultation, String status) {
        jdbc.update(
                "INSERT INTO payments (id, consultation_id, amount, status) VALUES (?, ?, 100.00, ?)",
                UUID.randomUUID(), consultation, status);
    }

    @Test
    void stuckConsultationIsFullyCompensated() {
        UUID slot = seedSlot("LOCKED");
        UUID c = seedConsultation(slot, "PENDING_PAYMENT", 30);
        seedPayment(c, "PAYING");

        orchestrator.reconcile();

        assertEquals("CANCELLED",
                jdbc.queryForObject("SELECT status FROM consultations WHERE id = ?", String.class, c));
        assertEquals("FAILED",
                jdbc.queryForObject("SELECT status FROM payments WHERE consultation_id = ?",
                        String.class, c));
        assertEquals("AVAILABLE",
                jdbc.queryForObject("SELECT status FROM availability_slots WHERE id = ?",
                        String.class, slot));
        assertEquals(null,
                jdbc.queryForObject("SELECT locked_by_id FROM availability_slots WHERE id = ?",
                        String.class, slot));
    }

    @Test
    void freshConsultationIsUntouched() {
        UUID slot = seedSlot("LOCKED");
        UUID c = seedConsultation(slot, "PENDING_PAYMENT", 1);
        seedPayment(c, "PAYING");

        orchestrator.reconcile();

        assertEquals("PENDING_PAYMENT",
                jdbc.queryForObject("SELECT status FROM consultations WHERE id = ?", String.class, c));
        assertEquals("PAYING",
                jdbc.queryForObject("SELECT status FROM payments WHERE consultation_id = ?",
                        String.class, c));
        assertEquals("LOCKED",
                jdbc.queryForObject("SELECT status FROM availability_slots WHERE id = ?",
                        String.class, slot));
    }
}
