package com.amrutam.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Body for admin analytics endpoints keyed by a date range.
 *
 * @param from range start (inclusive)
 * @param to range end (inclusive)
 */
public record DateRangeRequest(@NotNull LocalDate from, @NotNull LocalDate to) {
}
