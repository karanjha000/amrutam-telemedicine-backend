package com.amrutam.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A recorded step of a multi-step saga (e.g. the booking saga: create
 * consultation → lock slot → create payment → capture → notify).
 *
 * <p>Owned by the api module; written by {@code AsyncBridge.recordSagaStep}
 * so compensation can trace which step failed and roll back in reverse.</p>
 */
@Entity
@Table(name = "saga_steps")
public class SagaStep {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "saga_id", nullable = false)
    private UUID sagaId;

    @Column(name = "step_index", nullable = false)
    private int stepIndex;

    @Column(name = "step_name", nullable = false, length = 64)
    private String stepName;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(columnDefinition = "text")
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * @return id
     */
    public UUID getId() { return id; }

    /**
     * @param id id
     */
    public void setId(UUID id) { this.id = id; }

    /**
     * @return sagaId
     */
    public UUID getSagaId() { return sagaId; }

    /**
     * @param sagaId sagaId
     */
    public void setSagaId(UUID sagaId) { this.sagaId = sagaId; }

    /**
     * @return stepIndex
     */
    public int getStepIndex() { return stepIndex; }

    /**
     * @param stepIndex stepIndex
     */
    public void setStepIndex(int stepIndex) { this.stepIndex = stepIndex; }

    /**
     * @return stepName
     */
    public String getStepName() { return stepName; }

    /**
     * @param stepName stepName
     */
    public void setStepName(String stepName) { this.stepName = stepName; }

    /**
     * @return status
     */
    public String getStatus() { return status; }

    /**
     * @param status status
     */
    public void setStatus(String status) { this.status = status; }

    /**
     * @return detail
     */
    public String getDetail() { return detail; }

    /**
     * @param detail detail
     */
    public void setDetail(String detail) { this.detail = detail; }

    /**
     * @return createdAt
     */
    public Instant getCreatedAt() { return createdAt; }

    /**
     * @param createdAt createdAt
     */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
