package com.amrutam.api.entity;

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
 * A booked (or bookable) virtual-consultation session.
 */
@Entity
@Table(name = "consultations")
public class Consultation {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ConsultationStatus status;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(columnDefinition = "text")
    private String notes;

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
     * @return patientId
     */
    public UUID getPatientId() { return patientId; }

    /**
     * @param patientId patientId
     */
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    /**
     * @return doctorId
     */
    public UUID getDoctorId() { return doctorId; }

    /**
     * @param doctorId doctorId
     */
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }

    /**
     * @return slotId
     */
    public UUID getSlotId() { return slotId; }

    /**
     * @param slotId slotId
     */
    public void setSlotId(UUID slotId) { this.slotId = slotId; }

    /**
     * @return status
     */
    public ConsultationStatus getStatus() { return status; }

    /**
     * @param status status
     */
    public void setStatus(ConsultationStatus status) { this.status = status; }

    /**
     * @return scheduledAt
     */
    public Instant getScheduledAt() { return scheduledAt; }

    /**
     * @param scheduledAt scheduledAt
     */
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }

    /**
     * @return completedAt
     */
    public Instant getCompletedAt() { return completedAt; }

    /**
     * @param completedAt completedAt
     */
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    /**
     * @return notes
     */
    public String getNotes() { return notes; }

    /**
     * @param notes notes
     */
    public void setNotes(String notes) { this.notes = notes; }

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
