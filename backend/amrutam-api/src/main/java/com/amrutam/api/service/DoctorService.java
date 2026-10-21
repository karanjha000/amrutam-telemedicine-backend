package com.amrutam.api.service;

import com.amrutam.api.dto.AvailabilitySlotDto;
import com.amrutam.api.dto.DoctorCreateRequest;
import com.amrutam.api.dto.DoctorDto;
import com.amrutam.api.dto.SlotCreateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Doctor / availability service.
 */
public interface DoctorService {

    /**
     * Case-insensitive search over specialization / bio.
     *
     * @param query search text
     * @param pageable paging
     * @return matching page
     */
    Page<DoctorDto> search(String query, Pageable pageable);

    /**
     * @param id doctor id
     * @return the doctor, if present
     */
    Optional<DoctorDto> get(UUID id);

    /**
     * Creates a doctor account + row (admin only, enforced at the controller);
     * 409 on username clash.
     *
     * @param request doctor payload
     * @return the created doctor
     */
    DoctorDto create(DoctorCreateRequest request);

    /**
     * Availability window for a doctor (404 when the doctor is unknown).
     *
     * @param doctorId doctor
     * @param from window start
     * @param to window end
     * @param pageable paging
     * @return matching slots
     */
    Page<AvailabilitySlotDto> slotsFor(UUID doctorId, Instant from, Instant to, Pageable pageable);

    /**
     * Creates an availability slot for a doctor (409 on an overlapping
     * duplicate window, 404 on an unknown/inactive doctor).
     *
     * @param doctorId doctor
     * @param request slot payload
     * @return the created slot
     */
    AvailabilitySlotDto createSlot(UUID doctorId, SlotCreateRequest request);
}
