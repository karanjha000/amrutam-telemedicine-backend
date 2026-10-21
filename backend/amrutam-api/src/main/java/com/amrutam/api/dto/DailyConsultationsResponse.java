package com.amrutam.api.dto;

import java.util.Map;

/**
 * Admin analytics: consultations scheduled on a given day.
 *
 * @param date the day (ISO date string)
 * @param total total consultations (excluding cancelled)
 * @param perDoctor counts keyed by doctor id
 */
public record DailyConsultationsResponse(String date, long total, Map<String, Long> perDoctor) {
}
