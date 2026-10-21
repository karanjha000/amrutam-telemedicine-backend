package com.amrutam.api.service;

import com.amrutam.api.dto.DoctorDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Doctor search facade (thin delegation to {@code DoctorRepository}).
 */
public interface SearchService {

    /**
     * Case-insensitive doctor search.
     *
     * @param specialization optional specialization filter
     * @param query free-text query
     * @param pageable paging
     * @return matching page
     */
    Page<DoctorDto> searchDoctors(String specialization, String query, Pageable pageable);
}
