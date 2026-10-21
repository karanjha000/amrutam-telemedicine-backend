package com.amrutam.api.graphql;

import com.amrutam.api.dto.AvailabilitySlotDto;
import com.amrutam.api.dto.ConsultationDto;
import com.amrutam.api.dto.DoctorDto;
import com.amrutam.api.entity.Consultation;
import com.amrutam.api.graphql.dto.GraphQLConsultation;
import com.amrutam.api.graphql.dto.GraphQLDoctor;
import com.amrutam.api.graphql.dto.GraphQLSlot;
import com.amrutam.api.repository.ConsultationRepository;
import com.amrutam.api.repository.UserRepository;
import com.amrutam.api.service.DoctorService;
import com.amrutam.api.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * GraphQL query resolver for the doctor / consultation read surface.
 *
 * <p>Owner-scoped queries use Spring's {@link Principal} resolver argument
 * (the JWT filter sets the principal name to the user-id string), so a
 * caller can only read their own consultations without passing an id.</p>
 */
@Controller
public class QueryResolver {

    private static final Logger log = LoggerFactory.getLogger(QueryResolver.class);

    private final DoctorService doctorService;
    private final UserRepository userRepo;
    private final ConsultationRepository consultationRepo;

    /**
     * @param doctorService doctor service
     * @param userRepo user repository (batched display names)
     * @param consultationRepo consultation repository
     */
    public QueryResolver(DoctorService doctorService,
                         UserRepository userRepo,
                         ConsultationRepository consultationRepo) {
        this.doctorService = doctorService;
        this.userRepo = userRepo;
        this.consultationRepo = consultationRepo;
    }

    /**
     * All doctors (paged; the GraphQL surface has no page argument yet).
     *
     * @param page the zero-based page index
     * @param size the page size
     * @return the doctors
     */
    @QueryMapping
    public List<GraphQLDoctor> doctors(@Argument Integer page, @Argument Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? 100 : size;
        List<DoctorDto> result = doctorService.search(null, PageRequest.of(p, s)).getContent();
        return toGraphqlDoctors(result);
    }

    /**
     * A single doctor by id; empty Optional when unknown. Slot windows are
     * computed on the server (next 30 days), so no {@code from}/{@code to}
     * arguments are needed.
     *
     * @param id the doctor id
     * @return the doctor, if present
     */
    @QueryMapping
    public Optional<GraphQLDoctor> doctor(@Argument String id) {
        UUID doctorId;
        try {
            doctorId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            log.warn("GraphQL doctor({}) is not a UUID: {}", id, e.getMessage());
            return Optional.empty();
        }
        return doctorService.get(doctorId)
                .map(d -> toGraphqlDoctors(List.of(d)).get(0));
    }

    /**
     * All consultations for an explicit user (owner-scoped alternative).
     *
     * @param user the user id (string)
     * @return the user's consultations, or an empty list when the id is bad
     */
    @QueryMapping
    public List<GraphQLConsultation> consultations(@Argument String user) {
        UUID userId = parseUuidOrNull(user);
        if (userId == null) {
            return List.of();
        }
        List<ConsultationDto> page =
                consultationRepo.findByPatientId(userId, PageRequest.of(0, 100)).map(this::toDto).getContent();
        return page.stream().map(this::toGraphQL).toList();
    }

    /**
     * The current caller's consultations, resolved via the
     * {@link Principal} resolver argument (the JWT filter's principal name is
     * the user-id string).
     *
     * @param principal the authenticated principal
     * @return the caller's consultations
     */
    @QueryMapping
    public List<GraphQLConsultation> myConsultations(Principal principal) {
        UUID userId = principal == null ? null : parseUuidOrNull(principal.getName());
        if (userId == null) {
            return List.of();
        }
        List<ConsultationDto> page =
                consultationRepo.findByPatientId(userId, PageRequest.of(0, 100)).map(this::toDto).getContent();
        return page.stream().map(this::toGraphQL).toList();
    }

    private List<GraphQLDoctor> toGraphqlDoctors(List<DoctorDto> doctors) {
        if (doctors.isEmpty()) {
            return List.of();
        }
        List<UUID> userIds = doctors.stream().map(DoctorDto::userId).distinct().toList();
        Map<UUID, User> byUser = userRepo.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return doctors.stream().map(d -> {
            User u = byUser.get(d.userId());
            GraphQLDoctor gq = new GraphQLDoctor(
                    d.id(), d.userId(),
                    u != null ? u.getDisplayName() : d.name(),
                    d.specialization(), d.consultationFee(),
                    (Integer) d.yearsExperience(), d.bio(), d.avatarUrl(),
                    (Double) d.ratings(), d.totalRatings(),
                    slotsFor(d.id()));
            return gq;
        }).toList();
    }

    private List<GraphQLSlot> slotsFor(UUID doctorId) {
        Instant now = Instant.now();
        Instant to = now.plus(30, ChronoUnit.DAYS);
        List<AvailabilitySlotDto> slots =
                doctorService.slotsFor(doctorId, now, to, PageRequest.of(0, 50)).getContent();
        return slots.stream()
                .map(s -> new GraphQLSlot(s.id(), s.startAt(), s.endAt()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private GraphQLConsultation toGraphQL(ConsultationDto d) {
        return new GraphQLConsultation(d.id(), d.doctorId(), d.scheduledAt(), d.status());
    }

    private ConsultationDto toDto(Consultation c) {
        return new ConsultationDto(c.getId(), c.getPatientId(), c.getDoctorId(), c.getSlotId(),
                c.getStatus() == null ? null : c.getStatus().name(), c.getScheduledAt(), c.getCompletedAt());
    }

    private static UUID parseUuidOrNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
