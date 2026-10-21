package com.amrutam.api.graphql.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * GraphQL read model for an availability slot.
 *
 * @param id slot id
 * @param startAt window start
 * @param endAt window end
 */
public record GraphQLSlot(UUID id, Instant startAt, Instant endAt) {
}
