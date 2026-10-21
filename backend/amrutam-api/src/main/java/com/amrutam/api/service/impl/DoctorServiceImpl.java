package com.amrutam.api.service.impl;

import com.amrutam.api.audit.AuditRecorder;
import com.amrutam.api.dto.AvailabilitySlotDto;
import com.amrutam.api.dto.DoctorCreateRequest;
import com.amrutam.api.dto.DoctorDto;
import com.amrutam.api.dto.SlotCreateRequest;
import com.amrutam.api.entity.AvailabilitySlot;
import com.amrutam.api.entity.Doctor;
import com.amrutam.api.entity.SlotStatus;
import com.amrutam.api.entity.User;
import com.amrutam.api.repository.AvailabilitySlotRepository;
import com.amrutam.api.repository.DoctorRepository;
import com.amrutam.api.repository.UserRepository;
import com.amrutam.api.service.DoctorService;
import com.amrutam.common.exception.ConflictException;
import com.amrutam.common.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link DoctorService} implementation.
 *
 * <p>Doctor lookups batch the linked users per page via
 * {@code userRepository.findAllById} (a Spring Data standard query) instead
 * of issuing one query per doctor row, keeping search free of N+1 access.</p>
 */
@Service
public class DoctorServiceImpl implements DoctorService {

    private static final Logger log = LoggerFactory.getLogger(DoctorServiceImpl.class);

    private final DoctorRepository doctors;
    private final UserRepository users;
    private final AvailabilitySlotRepository slots;
    private final PasswordEncoder encoder;
    private final AuditRecorder audit;

    /**
     * @param doctors doctor repository
     * @param users user repository
     * @param slots availability repository
     * @param encoder password encoder
     * @param audit durable audit recorder
     */
    public DoctorServiceImpl(DoctorRepository doctors,
                             UserRepository users,
                             AvailabilitySlotRepository slots,
                             PasswordEncoder encoder,
                             AuditRecorder audit) {
        this.doctors = doctors;
        this.users = users;
        this.slots = slots;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Override
    @Cacheable("doctors")
    @Transactional(readOnly = true)
    public Page<DoctorDto> search(String query, Pageable pageable) {
        Page<Doctor> page = (query == null || query.isBlank())
                ? doctors.findAll(pageable)
                : doctors.searchDoctors(query, pageable);
        Map<UUID, User> byUser = usersByIds(page.getContent());
        return page.map(d -> toDto(d, byUser));
    }

    @Override
    @Cacheable(value = "doctors", key = "#id")
    @Transactional(readOnly = true)
    public Optional<DoctorDto> get(UUID id) {
        Doctor doctor = doctors.findById(id).orElse(null);
        if (doctor == null) {
            // An empty Optional must not be cached (null values are disabled
            // for the cache); fall through uncached.
            return Optional.empty();
        }
        Map<UUID, User> byUser = usersByIds(List.of(doctor));
        return Optional.of(toDto(doctor, byUser));
    }

    @Override
    @Transactional
    public DoctorDto create(DoctorCreateRequest request) {
        if (users.existsByUsername(request.username())
                || users.findByUsername(request.username().toLowerCase(Locale.ROOT)).isPresent()) {
            throw new ConflictException("username already taken: " + request.username());
        }
        Instant now = Instant.now();
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(encoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setEmail(request.email());
        user.setRole(User.RoleString.DOCTOR);
        user.setMfaEnabled(false);
        user.setActive(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        users.save(user);

        Doctor doctor = new Doctor();
        doctor.setUserId(user.getId());
        doctor.setSpecialization(request.specialization());
        doctor.setConsultationFee(request.consultationFee() != null ? request.consultationFee() : BigDecimal.ZERO);
        doctor.setYearsExperience(request.yearsExperience() != null ? request.yearsExperience() : 0);
        doctor.setBio(request.bio());
        doctor.setActive(true);
        doctor.setRatings(0.0);
        doctor.setTotalRatings(0);
        doctor.setCreatedAt(now);
        doctor.setUpdatedAt(now);
        doctors.save(doctor);

        audit.record(user.getId(), "doctor.create", "doctor:" + doctor.getId(), null,
                Map.of("username", request.username()));
        log.info("Created doctor id={} userId={}", doctor.getId(), user.getId());
        return toDto(doctor, Map.of());
    }

    @Override
    @Cacheable(value = "availability", key = "#doctorId + ':' + #from + ':' + #to")
    @Transactional(readOnly = true)
    public Page<AvailabilitySlotDto> slotsFor(UUID doctorId, Instant from, Instant to, Pageable pageable) {
        if (!doctors.existsById(doctorId)) {
            throw new NotFoundException("doctor", String.valueOf(doctorId));
        }
        return slots.findByDoctorIdAndStartAtBetween(doctorId, from, to, pageable).map(this::toSlotDto);
    }

    @Override
    @Transactional
    public AvailabilitySlotDto createSlot(UUID doctorId, SlotCreateRequest request) {
        Doctor doctor = doctors.findById(doctorId)
                .orElseThrow(() -> new NotFoundException("doctor", String.valueOf(doctorId)));
        if (!doctor.isActive()) {
            throw new ConflictException("doctor is not active: " + doctorId);
        }
        // Duplicate-window guard: the doctor must not already own a slot with
        // the exact same (startAt, endAt) — the table also carries a
        // (doctor_id, start_at, end_at) unique constraint as the backstop.
        boolean duplicate = slots.findByDoctorIdAndStartAtBetween(doctorId, request.startAt(),
                        request.endAt(), Pageable.ofSize(100))
                .getContent().stream()
                .anyMatch(s -> s.getStartAt().equals(request.startAt()) && s.getEndAt().equals(request.endAt()));
        if (duplicate) {
            throw new ConflictException("slot window already exists: " + request.startAt() + " - " + request.endAt());
        }
        Instant now = Instant.now();
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setDoctorId(doctorId);
        slot.setStartAt(request.startAt());
        slot.setEndAt(request.endAt());
        slot.setStatus(SlotStatus.AVAILABLE);
        slot.setCreatedAt(now);
        AvailabilitySlot saved = slots.save(slot);

        audit.record(doctor.getUserId(), "availability.create", "slot:" + saved.getId(), null,
                Map.of("doctorId", doctorId.toString(), "startAt", request.startAt().toString()));
        log.info("Created slot {} for doctor {}", saved.getId(), doctorId);
        return toSlotDto(saved);
    }

    /**
     * Batch-loads the users for a page of doctors in a single query.
     */
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

    private AvailabilitySlotDto toSlotDto(AvailabilitySlot s) {
        return new AvailabilitySlotDto(s.getId(), s.getDoctorId(), s.getStartAt(), s.getEndAt(),
                s.getStatus() == null ? SlotStatus.AVAILABLE.name() : s.getStatus().name());
    }
}
