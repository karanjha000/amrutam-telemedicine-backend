package com.amrutam.api.repository;

import com.amrutam.api.entity.Consultation;
import com.amrutam.api.entity.ConsultationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * JPA repository for {@link Consultation}.
 */
public interface ConsultationRepository extends JpaRepository<Consultation, UUID> {

    /**
     * @param patientId patient
     * @param pageable paging
     * @return the patient's consultations
     */
    Page<Consultation> findByPatientId(UUID patientId, Pageable pageable);

    /**
     * @param doctorId doctor
     * @param pageable paging
     * @return the doctor's consultations
     */
    Page<Consultation> findByDoctorId(UUID doctorId, Pageable pageable);

    /**
     * Count of consultations in a status window (analytics).
     *
     * @param status status
     * @param from window start
     * @param to window end
     * @return count
     */
    long countByStatusAndScheduledAtBetween(ConsultationStatus status, Instant from, Instant to);

    /**
     * Count of consultations in a set of statuses within a window (e.g.
     * active = PAID + COMPLETED in the last 24h).
     *
     * @param statuses statuses to count
     * @param from window start
     * @param to window end
     * @return count
     */
    @Query("select count(c) from Consultation c where c.status in :statuses "
            + "and c.scheduledAt between :from and :to")
    long countByStatusInAndScheduledAtBetween(@Param("statuses") List<ConsultationStatus> statuses,
                                              @Param("from") Instant from,
                                              @Param("to") Instant to);

    /**
     * Counts per doctor for a given day (daily-consultations analytics).
     *
     * @param dayFrom start of day (UTC)
     * @param dayTo end of day (UTC)
     * @return rows of [doctorId, count]
     */
    @Query("select c.doctorId, count(c) from Consultation c "
            + "where c.scheduledAt between :dayFrom and :dayTo "
            + "and c.status <> 'CANCELLED' group by c.doctorId")
    List<Object[]> countByDoctorScheduledAtBetween(@Param("dayFrom") Instant dayFrom, @Param("dayTo") Instant dayTo);
}
