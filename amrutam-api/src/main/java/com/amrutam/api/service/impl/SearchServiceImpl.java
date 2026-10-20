package com.amrutam.api.service.impl;

import com.amrutam.api.dto.AvailabilitySlotDto;
import com.amrutam.api.dto.DoctorDto;
import com.amrutam.api.entity.Doctor;
import com.amrutam.api.entity.User;
import com.amrutam.api.repository.DoctorRepository;
import com.amrutam.api.repository.UserRepository;
import com.amrutam.api.service.SearchService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link SearchService} implementation.
 */
@Service
public class SearchServiceImpl implements SearchService {

    private final DoctorRepository doctors;
    private final UserRepository users;

    /**
     * @param doctors doctor repository
     * @param users user repository
     */
    public SearchServiceImpl(DoctorRepository doctors, UserRepository users) {
        this.doctors = doctors;
        this.users = users;
    }

    @Override
    @Cacheable(value = "doctors", key = "#specialization + ':' + #query + ':' + #pageable.pageNumber")
    @Transactional(readOnly = true)
    public Page<DoctorDto> searchDoctors(String specialization, String query, Pageable pageable) {
        String q = (specialization == null || specialization.isBlank())
                ? (query == null || query.isBlank() ? "%" : query)
                : (query == null || query.isBlank() ? specialization : specialization + " " + query);
        Page<Doctor> page = doctors.searchDoctors(q, pageable);
        Map<UUID, User> byUser = usersByIds(page.getContent());
        return page.map(d -> toDto(d, byUser));
    }

    private Map<UUID, User> usersByIds(List<Doctor> doctorRows) {
        List<UUID> userIds = doctorRows.stream().map(Doctor::getUserId).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return users.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private DoctorDto toDto(Doctor d, Map<UUID, User> userIndex) {
        User linked = userIndex.get(d.getUserId());
        String name = linked != null ? linked.getDisplayName() : null;
        return new DoctorDto(d.getId(), d.getUserId(), name, d.getSpecialization(),
                d.getConsultationFee(), d.getYearsExperience(), d.getBio(), d.getAvatarUrl(),
                d.getRatings(), d.getTotalRatings(), null);
    }
}
