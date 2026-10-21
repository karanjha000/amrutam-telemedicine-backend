package com.amrutam.api.repository;

import com.amrutam.api.entity.AvailabilitySlot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link AvailabilitySlot}.
 *
 * <p>Locks are implemented with a {@code PESSIMISTIC_WRITE} row lock and a
 * status-guarded {@code @Modifying} update so that only {@code AVAILABLE}
 * slots can transition to {@code LOCKED} (exactly-one-winner semantics for
 * two patients racing on the same slot).</p>
 */
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, UUID> {

    /**
     * Slots for a doctor within a window.
     *
     * @param doctorId the doctor
     * @param from window start (inclusive)
     * @param to window end (inclusive)
     * @param pageable paging
     * @return matching slots
     */
    Page<AvailabilitySlot> findByDoctorIdAndStartAtBetween(UUID doctorId, Instant from, Instant to, Pageable pageable);

    /**
     * Acquires a {@code PESSIMISTIC_WRITE} row lock on the slot; must run in
     * an open transaction.
     *
     * @param slotId slot to lock
     * @return the locked slot, if present
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AvailabilitySlot s where s.id = :id")
    Optional<AvailabilitySlot> lockSlotForBooking(@Param("id") UUID slotId);

    /**
     * Atomic transition {@code AVAILABLE -> LOCKED}. Returns the number of
     * affected rows (0 if the slot was no longer available).
     *
     * @param id slot id
     * @param userId locking user
     * @param exp lock expiry
     * @return rows affected
     */
    @Modifying
    @Transactional
    @Query("update AvailabilitySlot s set s.status = 'LOCKED', s.lockedById = :userId, "
            + "s.lockExpiresAt = :exp where s.id = :id and s.status = 'AVAILABLE'")
    int lockSlot(@Param("id") UUID id, @Param("userId") UUID userId, @Param("exp") Instant exp);

    /**
     * Atomic transition back to {@code CANCELLED} (compensation).
     *
     * @param id slot id
     * @return rows affected
     */
    @Modifying
    @Transactional
    @Query("update AvailabilitySlot s set s.status = 'CANCELLED', s.lockedById = null, "
            + "s.lockExpiresAt = null where s.id = :id and s.status = 'LOCKED'")
    int unlockSlot(@Param("id") UUID id);
}
