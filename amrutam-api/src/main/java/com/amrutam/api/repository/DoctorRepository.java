package com.amrutam.api.repository;

import com.amrutam.api.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for {@link Doctor}.
 */
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    /**
     * Case-insensitive search over specialization and bio.
     *
     * @param q search text
     * @param pageable paging
     * @return matching page
     */
    @Query("select d from Doctor d where lower(d.specialization) like %lower(:q) "
            + "or lower(d.bio) like %lower(:q)")
    Page<Doctor> searchDoctors(@Param("q") String q, Pageable pageable);

    /**
     * @param userId the linked user id
     * @return all doctors backed by that user
     */
    List<Doctor> findByUserId(UUID userId);
}
