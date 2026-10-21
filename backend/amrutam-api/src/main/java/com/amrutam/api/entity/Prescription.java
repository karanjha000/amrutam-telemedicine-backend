package com.amrutam.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Prescription attached to a completed {@link Consultation}.
 *
 * <p>Medications are stored as a JSON string; structured parsing is left to
 * the UI layer for now.</p>
 */
@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "consultation_id", nullable = false, unique = true)
    private UUID consultationId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(columnDefinition = "text")
    private String medications;

    @Column(name = "dosage_notes", columnDefinition = "text")
    private String dosageNotes;

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
     * @return consultationId
     */
    public UUID getConsultationId() { return consultationId; }

    /**
     * @param consultationId consultationId
     */
    public void setConsultationId(UUID consultationId) { this.consultationId = consultationId; }

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
     * @return medications
     */
    public String getMedications() { return medications; }

    /**
     * @param medications medications
     */
    public void setMedications(String medications) { this.medications = medications; }

    /**
     * @return dosageNotes
     */
    public String getDosageNotes() { return dosageNotes; }

    /**
     * @param dosageNotes dosageNotes
     */
    public void setDosageNotes(String dosageNotes) { this.dosageNotes = dosageNotes; }

    /**
     * @return createdAt
     */
    public Instant getCreatedAt() { return createdAt; }

    /**
     * @param createdAt createdAt
     */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
