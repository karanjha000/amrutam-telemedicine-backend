package com.amrutam.api.service;

import com.amrutam.api.dto.BookConsultationResponse;
import com.amrutam.api.dto.BookingRequest;
import com.amrutam.api.dto.ConsultationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Booking saga service.
 *
 * <p><b>Idempotency semantics:</b> when the {@code X-Idempotency-Key} header
 * is present it <i>replaces</i> the body hash as the idempotency identity —
 * duplicate requests with the same key replay the stored result regardless of
 * body changes. Without the header the identity is the salted SHA-256 of the
 * serialized request body. A failing booking leaves the idempotency row
 * PENDING; it expires after the common kernel's 5-minute window and the
 * client may then retry.</p>
 */
public interface BookingService {

    /**
     * Books a consultation: create consultation, lock slot, create + capture
     * payment, enqueue the post-booking notification job.
     *
     * @param request booking payload
     * @param patientId the authenticated patient
     * @param explicitKey optional {@code X-Idempotency-Key} header
     * @return the booking result (with a {@code replayed} flag)
     */
    BookConsultationResponse book(BookingRequest request, UUID patientId, String explicitKey);

    /**
     * The caller's consultations (patient view).
     *
     * @param patientId patient
     * @param pageable paging
     * @return matching page
     */
    Page<ConsultationDto> myConsultations(UUID patientId, Pageable pageable);

    /**
     * Fetches a single consultation (404 when unknown).
     *
     * @param id consultation
     * @return the consultation dto
     */
    ConsultationDto get(UUID id);

    /**
     * Called by the payment worker in a future scope: marks a booking as
     * confirmed (future payment-webhook entry point). Stub.
     *
     * @param consultationId the consultation
     */
    void confirmBooking(UUID consultationId);

    /**
     * Cancels a consultation: releases the slot lock, marks an uncaptured
     * payment FAILED and the consultation CANCELLED. A captured payment is
     * left as-is for the future refund workflow.
     *
     * @param consultationId the consultation
     * @param actorId the requesting user (ownership check)
     */
    void cancel(UUID consultationId, UUID actorId);

    /**
     * Marks a consultation COMPLETED (doctor action). 409 unless the current
     * status is PAID (or PENDING_PAYMENT, which is allowed with a logged
     * TODO while the payment webhook is unimplemented).
     *
     * @param consultationId the consultation
     */
    void complete(UUID consultationId);
}
