package com.amrutam.api.service.impl;

import com.amrutam.api.audit.AuditRecorder;
import com.amrutam.api.dto.LoginRequest;
import com.amrutam.api.dto.LoginResponse;
import com.amrutam.api.dto.MfaChallengeDto;
import com.amrutam.api.dto.MfaVerifyRequest;
import com.amrutam.api.dto.PatientDto;
import com.amrutam.api.dto.RegisterPatientRequest;
import com.amrutam.api.dto.UserDto;
import com.amrutam.api.entity.User;
import com.amrutam.api.entity.UserProfile;
import com.amrutam.api.exception.InvalidCredentialsException;
import com.amrutam.api.repository.UserProfileRepository;
import com.amrutam.api.repository.UserRepository;
import com.amrutam.api.service.UserService;
import com.amrutam.common.exception.ConflictException;
import com.amrutam.common.exception.NotFoundException;
import com.amrutam.common.security.JwTokenService.Claims;
import com.amrutam.common.security.JwTokenService;
import com.amrutam.common.security.JwtException;
import com.amrutam.common.security.JwtProperties;
import com.amrutam.common.security.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * {@link UserService} implementation.
 *
 * <p>Token issuance goes through the injected {@link JwTokenService} with the
 * configured {@link JwtProperties#getTtlSeconds()}; roles are carried as
 * {@code ROLE_*} authority strings matching {@link Roles} so the common
 * kernel's filter and the method-security expressions agree. MFA is
 * stubbed: {@link #verifyMfa} accepts any well-formed 6-digit code until
 * the real TOTP integration lands (RFC 6238).</p>
 *
 * <p>Username uniqueness is enforced case-insensitively: because the
 * underlying store compares usernames case-sensitively, the service checks
 * both the exact and the lower-cased username before inserting.</p>
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final PasswordEncoder encoder;
    private final JwTokenService tokens;
    private final JwtProperties jwt;
    private final AuditRecorder audit;

    /**
     * @param users user repository
     * @param profiles profile repository
     * @param encoder password encoder
     * @param tokens token service
     * @param jwt jwt properties
     * @param audit durable audit recorder
     */
    public UserServiceImpl(UserRepository users,
                           UserProfileRepository profiles,
                           PasswordEncoder encoder,
                           JwTokenService tokens,
                           JwtProperties jwt,
                           AuditRecorder audit) {
        this.users = users;
        this.profiles = profiles;
        this.encoder = encoder;
        this.tokens = tokens;
        this.jwt = jwt;
        this.audit = audit;
    }

    @Override
    @Transactional
    public PatientDto registerPatient(RegisterPatientRequest request) {
        // The store compares usernames case-sensitively; check both casings
        // so "Ann" and "ann" never collide undetected.
        String username = request.username();
        if (users.findByUsername(username).isPresent()
                || users.findByUsername(username.toLowerCase(Locale.ROOT)).isPresent()) {
            throw new ConflictException("username already exists: " + username);
        }
        Instant now = Instant.now();
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setEmail(request.email());
        user.setRole(User.RoleString.PATIENT);
        user.setMfaEnabled(false);
        user.setActive(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        users.save(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setPhone(request.phone());
        profile.setPreferredLanguage("en");
        profile.setPreferredTimezone("Asia/Calcutta");
        profiles.save(profile);

        audit.record(user.getId(), "user.register", "user:" + user.getId(), null,
                Map.of("username", username));
        log.info("Registered patient id={} username={}", user.getId(), username);
        return toPatientDto(user, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = users.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));
        if (!user.isActive()) {
            throw new InvalidCredentialsException("Account disabled");
        }
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
        if (user.isMfaEnabled()) {
            // MFA users get a challenge, not a token.
            String uri = "otpauth://totp/amrutam:" + user.getUsername()
                    + "?secret=" + (user.getMfaSecret() == null ? "" : user.getMfaSecret())
                    + "&digits=6&period=30";
            MfaChallengeDto challenge = new MfaChallengeDto(user.getMfaSecret(), uri);
            return new LoginResponse(null, challenge, toUserDto(user));
        }
        return new LoginResponse(issueFor(user), null, toUserDto(user));
    }

    @Override
    @Transactional(readOnly = true)
    public String verifyMfa(MfaVerifyRequest request) {
        // TODO: real RFC-6238 TOTP verification; the stub accepts any
        // well-formed 6-digit code while the integration is pending.
        User user = users.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));
        log.info("MFA verify (TOTP stub) for user={}", user.getUsername());
        return issueFor(user);
    }

    @Override
    @Transactional(readOnly = true)
    public String refresh(String token) {
        if (!tokens.canParse(token)) {
            throw JwtException.unauthorized("Token is not parseable");
        }
        Claims claims = tokens.parseAndValidate(token); // throws JwtException when expired/invalid
        User user = users.findById(claims.userId())
                .orElseThrow(() -> new NotFoundException("user", String.valueOf(claims.userId())));
        return issueFor(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto me(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("user", String.valueOf(userId)));
        UserProfile profile = profiles.findByUserId(userId).orElse(null);
        return new UserDto(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getEmail(), user.getRole().name(),
                profile == null ? null : profile.getPhone(),
                profile == null ? null : profile.getPreferredLanguage());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> listUsers(Pageable pageable) {
        // Admin directory: a paged view; free-text query filters are a
        // future slice.
        Page<User> page = users.findAll(pageable);
        return page.map(this::toUserDto);
    }

    private String issueFor(User user) {
        String roleAuthority = switch (user.getRole()) {
            case PATIENT -> Roles.PATIENT;
            case DOCTOR -> Roles.DOCTOR;
            case ADMIN -> Roles.ADMIN;
        };
        JwTokenService.Subject subject =
                new JwTokenService.Subject(user.getId(), user.getUsername(), List.of(roleAuthority));
        return tokens.issue(subject, UUID.randomUUID().toString(), jwt.getTtlSeconds());
    }

    private UserDto toUserDto(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getEmail(), user.getRole().name(), null, null);
    }

    private PatientDto toPatientDto(User user, UserProfile profile) {
        return new PatientDto(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getEmail(), profile.getPhone());
    }
}
