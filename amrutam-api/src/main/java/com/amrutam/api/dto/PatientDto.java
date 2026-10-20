package com.amrutam.api.dto;

import java.util.UUID;

/**
 * Read-side representation of a patient account.
 *
 * @param id user id
 * @param username username
 * @param displayName display name
 * @param email email
 * @param phone phone (nullable until profile is filled)
 */
public record PatientDto(UUID id, String username, String displayName, String email, String phone) {
}
