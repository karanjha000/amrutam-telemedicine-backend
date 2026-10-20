package com.amrutam.api.graphql.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * GraphQL read model for a consultation.
 *
 * @param id consultation id
 * @param doctorId doctor
 * @param scheduledAt scheduled time
 * @param status status string
 */
public record GraphQLConsultation(UUID id, UUID doctorId, Instant scheduledAt, String status) {
}
