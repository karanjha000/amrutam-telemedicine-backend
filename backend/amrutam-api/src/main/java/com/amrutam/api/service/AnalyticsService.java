package com.amrutam.api.service;

import com.amrutam.api.dto.DailyConsultationsResponse;
import com.amrutam.api.dto.RevenueSummaryResponse;
import com.amrutam.api.dto.SystemHealthResponse;

import java.time.LocalDate;

/**
 * Admin analytics service.
 */
public interface AnalyticsService {

    /**
     * Consultations scheduled on a given day, grouped by doctor.
     *
     * @param date the day
     * @return the daily summary
     */
    DailyConsultationsResponse dailyConsultations(LocalDate date);

    /**
     * Captured revenue over a range.
     *
     * @param from range start (inclusive)
     * @param to range end (inclusive)
     * @return the revenue summary
     */
    RevenueSummaryResponse revenueSummary(LocalDate from, LocalDate to);

    /**
     * Coarse system-health snapshot.
     *
     * @return the health snapshot
     */
    SystemHealthResponse systemHealth();
}
