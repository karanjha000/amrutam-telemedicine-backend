package com.amrutam.api.dto;

import java.time.LocalDate;

/**
 * Patch payload for a user's profile. All fields are optional; null means
 * "leave unchanged".
 *
 * @param phone phone
 * @param dob date of birth
 * @param gender gender
 * @param preferredLanguage preferred language
 * @param preferredTimezone preferred timezone
 * @param bio bio
 * @param notes free-form notes
 */
public record ProfileUpdateRequest(String phone, LocalDate dob, String gender,
                                   String preferredLanguage, String preferredTimezone,
                                   String bio, String notes) {
}
