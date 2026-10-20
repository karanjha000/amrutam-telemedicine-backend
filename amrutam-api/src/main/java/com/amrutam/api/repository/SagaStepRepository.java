package com.amrutam.api.repository;

import com.amrutam.api.entity.SagaStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for {@link SagaStep}.
 */
public interface SagaStepRepository extends JpaRepository<SagaStep, UUID> {

    /**
     * All steps of a saga in execution order.
     *
     * @param sagaId saga
     * @return the steps
     */
    List<SagaStep> findBySagaIdOrderByStepIndexAsc(UUID sagaId);
}
