package com.amrutam.api.repository;

import com.amrutam.api.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link UserProfile}.
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    /**
     * @param userId the owning user id
     * @return the profile, if present
     */
    Optional<UserProfile> findByUserId(UUID userId);
}
