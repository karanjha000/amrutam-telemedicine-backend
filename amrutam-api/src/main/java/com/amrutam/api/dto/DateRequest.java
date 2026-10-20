package com.amrutam.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Body for admin analytics endpoints keyed by a single date.
 *
 * @param date the day
 */
public record DateRequest(@NotNull LocalDate date) {
}
