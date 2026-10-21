package com.amrutam.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit trail row.
 *
 * <p>Owned by the api module; the workers module appends via raw
 * {@code JdbcTemplate} (see {@code docs/ARCHITECTURE.md} for the JPA
 * ownership rule) to avoid double-defining entities across modules.</p>
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(nullable = false, length = 128)
    private String action;

    @Column(length = 128)
    private String target;

    @Column(length = 64)
    private String ip;

    @Column(columnDefinition = "text")
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /**
     * @return id
     */
    public UUID getId() { return id; }

    /**
     * @param id id
     */
    public void setId(UUID id) { this.id = id; }

    /**
     * @return actorId
     */
    public UUID getActorId() { return actorId; }

    /**
     * @param actorId actorId
     */
    public void setActorId(UUID actorId) { this.actorId = actorId; }

    /**
     * @return action
     */
    public String getAction() { return action; }

    /**
     * @param action action
     */
    public void setAction(String action) { this.action = action; }

    /**
     * @return target
     */
    public String getTarget() { return target; }

    /**
     * @param target target
     */
    public void setTarget(String target) { this.target = target; }

    /**
     * @return ip
     */
    public String getIp() { return ip; }

    /**
     * @param ip ip
     */
    public void setIp(String ip) { this.ip = ip; }

    /**
     * @return details
     */
    public String getDetails() { return details; }

    /**
     * @param details details
     */
    public void setDetails(String details) { this.details = details; }

    /**
     * @return occurredAt
     */
    public Instant getOccurredAt() { return occurredAt; }

    /**
     * @param occurredAt occurredAt
     */
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
