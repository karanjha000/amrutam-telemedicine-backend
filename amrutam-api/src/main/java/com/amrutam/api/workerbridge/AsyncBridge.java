package com.amrutam.api.workerbridge;

import java.util.Map;
import java.util.UUID;

/**
 * API-side seam for handing asynchronous work to the workers module.
 *
 * <p>The api module is the owner of the {@code async_jobs} table (JPA
 * entity + repository live here), so it enqueues work by writing rows
 * directly. The workers module picks them up via a lease/claim loop
 * ({@code findDueJobs} + {@code claimDueJobs}). When the workers module is
 * present and enabled ({@code amrutam.workers.enabled=true}) it registers its
 * own {@link AsyncBridge} bean which wins over the default JPA-backed one,
 * letting the two modules evolve the hand-off independently.</p>
 *
 * <p>Saga steps are recorded as rows in the shared {@code saga_steps} table
 * (owned here) so compensation can be traced across process boundaries.</p>
 */
public interface AsyncBridge {

    /**
     * Enqueues an asynchronous job for the workers.
     *
     * @param userId  owning user (scopes the job)
     * @param type    job type, e.g. {@code consultation.scheduled-notification}
     * @param payload job payload as a JSON-serializable map
     */
    void enqueueAsyncJob(UUID userId, String type, Map<String, Object> payload);

    /**
     * Records a saga step for observability / compensation.
     *
     * @param sagaId    saga identifier
     * @param stepIndex step ordinal
     * @param stepName  machine-readable step name
     * @param status    step status, e.g. {@code DONE}
     * @param detail    optional detail (JSON or free text)
     */
    void recordSagaStep(UUID sagaId, int stepIndex, String stepName, String status, String detail);
}
