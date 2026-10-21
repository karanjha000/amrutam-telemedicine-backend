package com.amrutam.api.dto;

import java.math.BigDecimal;

/**
 * Admin analytics: revenue within a window.
 *
 * @param totalCaptured sum of captured amounts (null when none)
 * @param capturedCount number of captured payments
 * @param currency currency of the totals
 */
public record RevenueSummaryResponse(BigDecimal totalCaptured, long capturedCount, String currency) {
}
