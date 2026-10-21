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
 * A bookable time window for a {@link Doctor}.
 *
 * <p>The natural key is {@code (doctor_id, start_at, end_at)}; the slot's
 * status (AVAILABLE / LOCKED / …) is guarded in code so that only one
 * LOCKED slot per window is ever created.</p>
 */
@Entity
@Table(name = "availability_slots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"doctor_id", "start_at", "end_at"}))
public class AvailabilitySlot {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SlotStatus status;

    @Column(name = "locked_by_id")
    private UUID lockedById;

    @Column(name = "lock_expires_at")
    private Instant lockExpiresAt;

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
     * @return doctorId
     */
    public UUID getDoctorId() { return doctorId; }

    /**
     * @param doctorId doctorId
     */
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }

    /**
     * @return startAt
     */
    public Instant getStartAt() { return startAt; }

    /**
     * @param startAt startAt
     */
    public void setStartAt(Instant startAt) { this.startAt = startAt; }

    /**
     * @return endAt
     */
    public Instant getEndAt() { return endAt; }

    /**
     * @param endAt endAt
     */
    public void setEndAt(Instant endAt) { this.endAt = endAt; }

    /**
     * @return status
     */
    public SlotStatus getStatus() { return status; }

    /**
     * @param status status
     */
    public void setStatus(SlotStatus status) { this.status = status; }

    /**
     * @return lockedById
     */
    public UUID getLockedById() { return lockedById; }

    /**
     * @param lockedById lockedById
     */
    public void setLockedById(UUID lockedById) { this.lockedById = lockedById; }

    /**
     * @return lockExpiresAt
     */
    public Instant getLockExpiresAt() { return lockExpiresAt; }

    /**
     * @param lockExpiresAt lockExpiresAt
     */
    public void setLockExpiresAt(Instant lockExpiresAt) { this.lockExpiresAt = lockExpiresAt; }

    /**
     * @return createdAt
     */
    public Instant getCreatedAt() { return createdAt; }

    /**
     * @param createdAt createdAt
     */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
