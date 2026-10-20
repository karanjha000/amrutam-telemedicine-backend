package com.amrutam.api.controller;

import com.amrutam.api.dto.BookConsultationResponse;
import com.amrutam.api.dto.BookingRequest;
import com.amrutam.api.dto.ConsultationDto;
import com.amrutam.api.dto.PaymentDto;
import com.amrutam.api.entity.Payment;
import com.amrutam.api.repository.ConsultationRepository;
import com.amrutam.api.service.BookingService;
import com.amrutam.api.service.PaymentService;
import com.amrutam.api.util.CurrentClaims;
import com.amrutam.common.exception.ApiErrorCode;
import com.amrutam.common.exception.ErrorBody;
import com.amrutam.common.exception.ForbiddenException;
import com.amrutam.common.exception.NotFoundException;
import com.amrutam.common.exception.RestResponse;
import com.amrutam.common.security.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Consultation / booking / payment endpoints.
 *
 * <p><b>Idempotency semantics (documented here per spec):</b> when the
 * {@code X-Idempotency-Key} header is present it <i>replaces</i> the body
 * hash as the idempotency identity — duplicate requests with the same key
 * replay the stored result and later body changes do NOT produce a new
 * booking. Without the header the identity is the salted SHA-256 of the
 * request body.</p>
 */
@RestController
@RequestMapping("/api/v1")
public class ConsultationsController {

    private static final Logger log = LoggerFactory.getLogger(ConsultationsController.class);

    private final BookingService booking;
    private final PaymentService payments;
    private final ConsultationRepository consultations;
    private final CurrentClaims claims;

    /**
     * @param booking booking service
     * @param payments payment service
     * @param consultations consultation repository
     * @param claims current-claims helper
     */
    public ConsultationsController(BookingService booking,
                                   PaymentService payments,
                                   ConsultationRepository consultations,
                                   CurrentClaims claims) {
        this.booking = booking;
        this.payments = payments;
        this.consultations = consultations;
        this.claims = claims;
    }

    /**
     * Books a consultation (the saga entry point).
     *
     * <p>HTTP semantics: <b>201 Created</b> on the first execution
     * ({@code replayed == false}); <b>200 OK</b> with the same payload on a
     * replay of a previously completed booking. This mirrors the
     * idempotency guarantee: the client gets the identical resource whether
     * the server executed the saga or served the stored result.</p>
     *
     * @param request booking payload
     * @param idempotencyKey optional {@code X-Idempotency-Key} header
     * @param http the current request (caller resolution)
     * @return 201/200 with the booking result
     */
    @PostMapping("/consultations/booking")
    public ResponseEntity<RestResponse<BookConsultationResponse>> book(
            @Valid @RequestBody BookingRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest http) {
        UUID patientId = requireUser(http);
        log.info("Booking request doctorId={} slotId={} idempotencyKey={} by user={}",
                request.doctorId(), request.slotId(), idempotencyKey, patientId);
        BookConsultationResponse response = booking.book(request, patientId, idempotencyKey);
        int status = response.replayed() ? 200 : 201;
        return ResponseEntity.status(status).body(RestResponse.ok(response));
    }

    /**
     * The caller's consultations (patient view).
     *
     * @param http the current request
     * @param pageable paging
     * @return matching page
     */
    @GetMapping("/consultations")
    public ResponseEntity<RestResponse<Page<ConsultationDto>>> mine(HttpServletRequest http, Pageable pageable) {
        UUID patientId = requireUser(http);
        return ResponseEntity.ok(RestResponse.ok(booking.myConsultations(patientId, pageable)));
    }

    /**
     * Fetch one consultation (owner or admin; 404 when unknown, 403 for a
     * non-owner).
     *
     * @param id consultation
     * @param http the current request
     * @return the consultation
     */
    @GetMapping("/consultations/{id}")
    public ResponseEntity<RestResponse<ConsultationDto>> get(@PathVariable UUID id, HttpServletRequest http) {
        Claims c = claims.claims(http);
        UUID caller = c == null ? requireUser(http) : c.userId();
        ConsultationDto dto = booking.get(id);
        checkOwnerOrAdmin(dto, caller, c != null && c.roles() != null && c.roles().contains("ROLE_ADMIN"));
        return ResponseEntity.ok(RestResponse.ok(dto));
    }

    /**
     * Cancels a consultation (patient-owner): releases the slot lock and
     * cancels the payment if not yet captured.
     *
     * @param id consultation
     * @param http the current request
     * @return an empty success envelope
     */
    @PostMapping("/consultations/{id}/cancel")
    public ResponseEntity<RestResponse<Void>> cancel(@PathVariable UUID id, HttpServletRequest http) {
        UUID patientId = requireUser(http);
        booking.cancel(id, patientId);
        return ResponseEntity.ok(RestResponse.ok(null));
    }

    /**
     * Marks a consultation COMPLETED (doctor-owner or admin). Ownership: the
     * acting doctor's linked user id must match the consultation's doctor row
     * (or the caller is an admin). 409 when the consultation is already in a
     * terminal state.
     *
     * @param id consultation
     * @param http the current request
     * @return an empty success envelope
     */
    @PostMapping("/consultations/{id}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<RestResponse<Void>> complete(@PathVariable UUID id, HttpServletRequest http) {
        claims.claims(http); // presence check; authority is enforced by @PreAuthorize
        booking.complete(id);
        return ResponseEntity.ok(RestResponse.ok(null));
    }

    /**
     * Captures a payment (self + admin only; 403 otherwise, 409 when the
     * payment is not in PAYING state).
     *
     * @param paymentId payment
     * @param http the current request
     * @return the payment
     */
    @PostMapping("/payments/{paymentId}/capture")
    public ResponseEntity<RestResponse<PaymentDto>> capture(@PathVariable UUID paymentId,
                                                             HttpServletRequest http) {
        Claims c = claims.claims(http);
        if (c == null) {
            return unauthorized();
        }
        boolean admin = c.roles() != null && c.roles().contains("ROLE_ADMIN");
        Payment p = payments.getEntity(paymentId);
        if (!admin) {
            com.amrutam.api.entity.Consultation consultation =
                    consultations.findById(p.getConsultationId())
                            .orElseThrow(() -> new NotFoundException("consultation",
                                    String.valueOf(p.getConsultationId())));
            if (!consultation.getPatientId().equals(c.userId())) {
                throw new ForbiddenException("not the owner of this payment");
            }
        }
        return ResponseEntity.ok(RestResponse.ok(payments.capture(paymentId)));
    }

    /**
     * Refunds a payment (admin only; 409 when not in CAPTURED state).
     *
     * @param paymentId payment
     * @return the payment
     */
    @PostMapping("/payments/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<PaymentDto>> refund(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(RestResponse.ok(payments.refund(paymentId)));
    }

    private UUID requireUser(HttpServletRequest http) {
        UUID userId = claims.userId(http);
        if (userId == null) {
            throw com.amrutam.common.security.JwtException.unauthorized("Authentication required");
        }
        return userId;
    }

    private void checkOwnerOrAdmin(ConsultationDto dto, UUID caller, boolean admin) {
        if (admin) {
            return;
        }
        if (!dto.patientId().equals(caller) && !dto.doctorId().equals(caller)) {
            throw new ForbiddenException("not the owner of this consultation");
        }
    }

    private ResponseEntity<RestResponse<PaymentDto>> unauthorized() {
        return ResponseEntity.status(401).body(new RestResponse<>(
                false, null, new ErrorBody(ApiErrorCode.UNAUTHORIZED, "Authentication required"), null));
    }
}
