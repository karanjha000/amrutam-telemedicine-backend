package com.amrutam.api.dto;

/**
 * Admin analytics: coarse system-health snapshot.
 *
 * @param activeSessions consultations in PAID + COMPLETED in the last 24h
 * @param pendingAsyncJobs PENDING rows in {@code async_jobs}
 * @param deadLetters DEAD rows in {@code async_jobs}
 */
public record SystemHealthResponse(long activeSessions, long pendingAsyncJobs, long deadLetters) {
}
