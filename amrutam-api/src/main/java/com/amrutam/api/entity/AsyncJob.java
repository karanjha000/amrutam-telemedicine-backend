package com.amrutam.api.entity;

/**
 * <b>Consumed by amrutam-workers via AsyncBridge.</b>
 *
 * <p>Ownership rule (see {@code docs/ARCHITECTURE.md}): the api module owns
 * this entity and its repository. The workers module does NOT redeclare it;
 * instead it consumes {@code async_jobs} rows through the shared
 * {@code AsyncBridge} SPI (JdbcTemplate or a read-only minimal mapping) and
 * writes status transitions through the same repository the api exposes.
 * This avoids double-definition of the same table in two JPA metamodels.</p>
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A unit of asynchronous work enqueued by the api and claimed by workers.
 */
@Entity
@Table(name = "async_jobs")
public class AsyncJob {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 64)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JobStatus status;

    @Column(columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private int attempt;

    @Column(name = "max_attempt", nullable = false)
    private int maxAttempt;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "lease_owner", length = 128)
    private String leaseOwner;

    @Column(name = "lease_expiry")
    private Instant leaseExpiry;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * @return id
     */
    public UUID getId() { return id; }

    /**
     * @param id id
     */
    public void setId(UUID id) { this.id = id; }

    /**
     * @return type
     */
    public String getType() { return type; }

    /**
     * @param type type
     */
    public void setType(String type) { this.type = type; }

    /**
     * @return status
     */
    public JobStatus getStatus() { return status; }

    /**
     * @param status status
     */
    public void setStatus(JobStatus status) { this.status = status; }

    /**
     * @return payload
     */
    public String getPayload() { return payload; }

    /**
     * @param payload payload
     */
    public void setPayload(String payload) { this.payload = payload; }

    /**
     * @return attempt
     */
    public int getAttempt() { return attempt; }

    /**
     * @param attempt attempt
     */
    public void setAttempt(int attempt) { this.attempt = attempt; }

    /**
     * @return maxAttempt
     */
    public int getMaxAttempt() { return maxAttempt; }

    /**
     * @param maxAttempt maxAttempt
     */
    public void setMaxAttempt(int maxAttempt) { this.maxAttempt = maxAttempt; }

    /**
     * @return scheduledAt
     */
    public Instant getScheduledAt() { return scheduledAt; }

    /**
     * @param scheduledAt scheduledAt
     */
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }

    /**
     * @return lastRunAt
     */
    public Instant getLastRunAt() { return lastRunAt; }

    /**
     * @param lastRunAt lastRunAt
     */
    public void setLastRunAt(Instant lastRunAt) { this.lastRunAt = lastRunAt; }

    /**
     * @return nextRunAt
     */
    public Instant getNextRunAt() { return nextRunAt; }

    /**
     * @param nextRunAt nextRunAt
     */
    public void setNextRunAt(Instant nextRunAt) { this.nextRunAt = nextRunAt; }

    /**
     * @return leaseOwner
     */
    public String getLeaseOwner() { return leaseOwner; }

    /**
     * @param leaseOwner leaseOwner
     */
    public void setLeaseOwner(String leaseOwner) { this.leaseOwner = leaseOwner; }

    /**
     * @return leaseExpiry
     */
    public Instant getLeaseExpiry() { return leaseExpiry; }

    /**
     * @param leaseExpiry leaseExpiry
     */
    public void setLeaseExpiry(Instant leaseExpiry) { this.leaseExpiry = leaseExpiry; }

    /**
     * @return createdAt
     */
    public Instant getCreatedAt() { return createdAt; }

    /**
     * @param createdAt createdAt
     */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /**
     * @return updatedAt
     */
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * @param updatedAt updatedAt
     */
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
