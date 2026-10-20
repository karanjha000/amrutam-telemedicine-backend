package com.amrutam.api.dto;

import java.util.UUID;

/**
 * Read-side representation of a user.
 *
 * @param id user id
 * @param username username
 * @param displayName display name
 * @param email email
 * @param role role string (PATIENT / DOCTOR / ADMIN)
 */
public record UserDto(UUID id, String username, String displayName, String email, String role) {
}
