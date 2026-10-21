package com.amrutam.api.repository;

import com.amrutam.api.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Prescription}.
 */
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

    /**
     * @param consultationId consultation
     * @return the prescription, if present
     */
    Optional<Prescription> findByConsultationId(UUID consultationId);
}
