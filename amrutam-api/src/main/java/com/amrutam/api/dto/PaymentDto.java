package com.amrutam.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-side representation of a payment.
 *
 * @param id payment id
 * @param consultationId consultation
 * @param amount amount
 * @param currency currency (default INR)
 * @param status status string
 * @param gatewayRef gateway reference (nullable)
 */
public record PaymentDto(UUID id, UUID consultationId, BigDecimal amount, String currency,
                         String status, String gatewayRef) {
}
