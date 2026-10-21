package com.amrutam.api.graphql.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * GraphQL read model for a doctor.
 *
 * @param id doctor id
 * @param userId linked user id
 * @param name display name
 * @param specialization specialization
 * @param consultationFee fee per consultation
 * @param yearsExperience years of experience
 * @param bio bio
 * @param avatarUrl avatar url
 * @param rating average rating (nullable until there are ratings)
 * @param totalRatings number of ratings
 * @param slots optional availability window (populated by the resolver)
 */
public record GraphQLDoctor(UUID id,
                            UUID userId,
                            String name,
                            String specialization,
                            BigDecimal consultationFee,
                            Integer yearsExperience,
                            String bio,
                            String avatarUrl,
                            Double rating,
                            Integer totalRatings,
                            List<GraphQLSlot> slots) {
}
