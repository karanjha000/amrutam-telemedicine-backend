package com.amrutam.api.controller;

import com.amrutam.api.dto.DailyConsultationsResponse;
import com.amrutam.api.dto.DateRangeRequest;
import com.amrutam.api.dto.DateRequest;
import com.amrutam.api.dto.RevenueSummaryResponse;
import com.amrutam.api.dto.SystemHealthResponse;
import com.amrutam.api.service.AnalyticsService;
import com.amrutam.common.exception.RestResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin analytics endpoints (gated on {@code ROLE_ADMIN}).
 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AnalyticsService analytics;

    /**
     * @param analytics analytics service
     */
    public AdminAnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    /**
     * Consultations scheduled on a day, grouped by doctor.
     *
     * @param body the day
     * @return the daily summary
     */
    @PostMapping("/daily-consultations")
    public ResponseEntity<RestResponse<DailyConsultationsResponse>> daily(
            @Valid @RequestBody DateRequest body) {
        return ResponseEntity.ok(RestResponse.ok(analytics.dailyConsultations(body.date())));
    }

    /**
     * Captured revenue over a range.
     *
     * @param body the range
     * @return the revenue summary
     */
    @PostMapping("/revenue-summary")
    public ResponseEntity<RestResponse<RevenueSummaryResponse>> revenue(
            @Valid @RequestBody DateRangeRequest body) {
        return ResponseEntity.ok(RestResponse.ok(
                analytics.revenueSummary(body.from(), body.to())));
    }

    /**
     * Coarse system-health snapshot.
     *
     * @return the health snapshot
     */
    @PostMapping("/health")
    public ResponseEntity<RestResponse<SystemHealthResponse>> health() {
        return ResponseEntity.ok(RestResponse.ok(analytics.systemHealth()));
    }
}
