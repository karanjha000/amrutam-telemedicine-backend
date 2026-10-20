package com.amrutam.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A payment intent for a {@link Consultation}, settled by a payment gateway.
 *
 * <p>{@code idempotencyKey} is unique so that duplicate booking requests are
 * never billed twice: a second call with the same key returns the stored
 * payment instead of creating a new one.</p>
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "consultation_id", nullable = false)
    private UUID consultationId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 8)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentStatus status;

    @Column(name = "gateway_ref", length = 128)
    private String gatewayRef;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

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
     * @return consultationId
     */
    public UUID getConsultationId() { return consultationId; }

    /**
     * @param consultationId consultationId
     */
    public void setConsultationId(UUID consultationId) { this.consultationId = consultationId; }

    /**
     * @return amount
     */
    public BigDecimal getAmount() { return amount; }

    /**
     * @param amount amount
     */
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    /**
     * @return currency
     */
    public String getCurrency() { return currency; }

    /**
     * @param currency currency
     */
    public void setCurrency(String currency) { this.currency = currency; }

    /**
     * @return status
     */
    public PaymentStatus getStatus() { return status; }

    /**
     * @param status status
     */
    public void setStatus(PaymentStatus status) { this.status = status; }

    /**
     * @return gatewayRef
     */
    public String getGatewayRef() { return gatewayRef; }

    /**
     * @param gatewayRef gatewayRef
     */
    public void setGatewayRef(String gatewayRef) { this.gatewayRef = gatewayRef; }

    /**
     * @return idempotencyKey
     */
    public String getIdempotencyKey() { return idempotencyKey; }

    /**
     * @param idempotencyKey idempotencyKey
     */
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

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
