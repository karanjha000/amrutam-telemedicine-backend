package com.amrutam.common.audit;

import java.util.UUID;

/**
 * Pure-data view of the actor for an audit record.
 *
 * @param id   the actor user id
 * @param role the primary role (e.g. {@code ROLE_PATIENT})
 */
public record AuditActor(UUID id, String role) {
}
