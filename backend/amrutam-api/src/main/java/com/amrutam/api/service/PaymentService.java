package com.amrutam.api.service;

import com.amrutam.api.dto.PaymentDto;
import com.amrutam.api.entity.Consultation;
import com.amrutam.api.entity.Payment;

import java.util.UUID;

/**
 * Payment lifecycle service (gateway capture is simulated in this scope).
 */
public interface PaymentService {

    /**
     * Creates a payment for a consultation using the doctor's consultation
     * fee. If an {@code idempotencyKey} is supplied and a matching payment
     * already exists, the existing payment is returned instead of a
     * duplicate (never a second charge).
     *
     * @param consultation the consultation to pay for
     * @param idempotencyKey optional idempotency key (a fresh UUID is
     *                       generated when null/blank)
     * @param paymentToken gateway payment token (nullable; capture is
     *                     simulated)
     * @return the payment (status PAYING)
     */
    PaymentDto createPayment(Consultation consultation, String idempotencyKey, String paymentToken);

    /**
     * Marks a payment as CAPTURED (PAYING -&gt; CAPTURED). Idempotent when the
     * payment is already CAPTURED; 409 otherwise.
     *
     * @param paymentId payment
     * @return the payment
     */
    PaymentDto capture(UUID paymentId);

    /**
     * Marks a payment as REFUNDED (CAPTURED -&gt; REFUNDED). Idempotent when
     * already REFUNDED; 409 otherwise.
     *
     * @param paymentId payment
     * @return the payment
     */
    PaymentDto refund(UUID paymentId);

    /**
     * Loads a payment by id (404 when unknown).
     *
     * @param paymentId payment
     * @return the payment dto
     */
    PaymentDto byId(UUID paymentId);

    /**
     * Loads the payment entity (used for compensation paths; 404 when unknown).
     *
     * @param paymentId payment
     * @return the payment entity
     */
    Payment getEntity(UUID paymentId);
}
