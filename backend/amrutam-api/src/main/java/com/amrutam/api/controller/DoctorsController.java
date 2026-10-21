package com.amrutam.api.controller;

import com.amrutam.api.dto.AvailabilitySlotDto;
import com.amrutam.api.dto.DoctorCreateRequest;
import com.amrutam.api.dto.DoctorDto;
import com.amrutam.api.dto.SlotCreateRequest;
import com.amrutam.api.repository.DoctorRepository;
import com.amrutam.api.service.DoctorService;
import com.amrutam.api.util.CurrentClaims;
import com.amrutam.common.exception.ForbiddenException;
import com.amrutam.common.exception.NotFoundException;
import com.amrutam.common.exception.RestResponse;
import com.amrutam.common.security.JwTokenService.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Doctor catalog + availability endpoints.
 *
 * <p>Slot creation is open to admins and doctor-owners; the ownership check
 * (a {@code ROLE_DOCTOR} caller may only open slots on their own linked
 * doctor row) is enforced via {@link CurrentClaims} +
 * {@link DoctorRepository#findByUserId} so a doctor cannot open slots for a
 * colleague.</p>
 */
@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorsController {

    private static final Logger log = LoggerFactory.getLogger(DoctorsController.class);

    private final DoctorService doctors;
    private final DoctorRepository doctorRepo;
    private final CurrentClaims claims;

    /**
     * @param doctors doctor service
     * @param doctorRepo doctor repository (ownership lookup)
     * @param claims current-claims helper
     */
    public DoctorsController(DoctorService doctors, DoctorRepository doctorRepo, CurrentClaims claims) {
        this.doctors = doctors;
        this.doctorRepo = doctorRepo;
        this.claims = claims;
    }

    /**
     * Search doctors.
     *
     * @param q free-text query
     * @param pageable paging
     * @return matching page
     */
    @GetMapping
    public ResponseEntity<RestResponse<Page<DoctorDto>>> search(
            @RequestParam(required = false) String q, Pageable pageable) {
        return ResponseEntity.ok(RestResponse.ok(doctors.search(q, pageable)));
    }

    /**
     * Fetch one doctor (404 when unknown).
     *
     * @param id doctor id
     * @return the doctor
     */
    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<DoctorDto>> get(@PathVariable UUID id) {
        DoctorDto doctor = doctors.get(id)
                .orElseThrow(() -> new NotFoundException("doctor", id.toString()));
        return ResponseEntity.ok(RestResponse.ok(doctor));
    }

    /**
     * Creates a doctor (admin only).
     *
     * @param request doctor payload
     * @return the created doctor
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<DoctorDto>> create(@Valid @RequestBody DoctorCreateRequest request) {
        return ResponseEntity.ok(RestResponse.ok(doctors.create(request)));
    }

    /**
     * Availability window for a doctor.
     *
     * @param id doctor
     * @param from window start
     * @param to window end
     * @param pageable paging
     * @return matching slots
     */
    @GetMapping("/{id}/availability")
    public ResponseEntity<RestResponse<Page<AvailabilitySlotDto>>> availability(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Pageable pageable) {
        return ResponseEntity.ok(RestResponse.ok(doctors.slotsFor(id, from, to, pageable)));
    }

    /**
     * Creates a slot for a doctor (admin, or the doctor-owner; 409 on an
     * overlapping duplicate window, 404 on an unknown doctor).
     *
     * @param id doctor
     * @param request slot payload
     * @param http the current request (claims resolution)
     * @return the created slot (201)
     */
    @PostMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public ResponseEntity<RestResponse<AvailabilitySlotDto>> createSlot(@PathVariable UUID id,
                                                                         @Valid @RequestBody SlotCreateRequest request,
                                                                         HttpServletRequest http) {
        enforceOwnership(id, http);
        AvailabilitySlotDto slot = doctors.createSlot(id, request);
        log.info("Created slot {} for doctor {} via POST", slot.id(), id);
        return ResponseEntity.status(201).body(RestResponse.ok(slot));
    }

    /**
     * Doctors may only open slots on their own linked row; admins are
     * unrestricted. 403 on a doctor acting on a foreign row.
     */
    private void enforceOwnership(UUID doctorId, HttpServletRequest http) {
        Claims c = claims.claims(http);
        if (c == null) {
            throw new ForbiddenException("Authentication required");
        }
        List<String> roles = c.roles() == null ? List.of() : c.roles();
        if (roles.contains("ROLE_ADMIN")) {
            return;
        }
        boolean owns = doctorRepo.findByUserId(c.userId()).stream().anyMatch(d -> d.getId().equals(doctorId));
        if (!owns) {
            throw new ForbiddenException("you may only manage slots for your own doctor profile");
        }
    }
}
