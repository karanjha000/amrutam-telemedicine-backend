package com.amrutam.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Idempotency key: records that a request {@code (user, method, path,
 * bodyHash)} was processed, and stores the stored result so replays can be
 * served without side effects.
 */
@Entity
@Table(name = "idempotency_keys",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "method", "path", "body_hash"}))
public class IdempotencyKey {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 255)
    private String path;

    @Column(name = "body_hash", nullable = false, length = 128)
    private String bodyHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IdempotencyState state;

    @Column(name = "result_json", columnDefinition = "text")
    private String resultJson;

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
     * @return userId
     */
    public UUID getUserId() { return userId; }

    /**
     * @param userId userId
     */
    public void setUserId(UUID userId) { this.userId = userId; }

    /**
     * @return method
     */
    public String getMethod() { return method; }

    /**
     * @param method method
     */
    public void setMethod(String method) { this.method = method; }

    /**
     * @return path
     */
    public String getPath() { return path; }

    /**
     * @param path path
     */
    public void setPath(String path) { this.path = path; }

    /**
     * @return bodyHash
     */
    public String getBodyHash() { return bodyHash; }

    /**
     * @param bodyHash bodyHash
     */
    public void setBodyHash(String bodyHash) { this.bodyHash = bodyHash; }

    /**
     * @return state
     */
    public IdempotencyState getState() { return state; }

    /**
     * @param state state
     */
    public void setState(IdempotencyState state) { this.state = state; }

    /**
     * @return resultJson
     */
    public String getResultJson() { return resultJson; }

    /**
     * @param resultJson resultJson
     */
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }

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
