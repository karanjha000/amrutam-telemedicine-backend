package com.amrutam.api.controller;

import com.amrutam.api.dto.ProfileUpdateRequest;
import com.amrutam.api.dto.UserDto;
import com.amrutam.api.entity.UserProfile;
import com.amrutam.api.repository.UserProfileRepository;
import com.amrutam.api.service.UserService;
import com.amrutam.api.util.CurrentClaims;
import com.amrutam.common.exception.ApiErrorCode;
import com.amrutam.common.exception.ErrorBody;
import com.amrutam.common.exception.ForbiddenException;
import com.amrutam.common.exception.NotFoundException;
import com.amrutam.common.exception.RestResponse;
import com.amrutam.common.security.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * User self-service and admin list endpoints.
 *
 * <p>"Who am I" resolution goes through {@link CurrentClaims} (the request
 * attribute published by the JWT filter) instead of re-deriving the user
 * from the {@code Authentication} principal name, so the caller id is the
 * validated {@code userId} claim and never a spoofable string.</p>
 */
@RestController
@RequestMapping("/api/v1/users")
public class UsersController {

    private static final Logger log = LoggerFactory.getLogger(UsersController.class);

    private final UserService users;
    private final UserProfileRepository profiles;
    private final CurrentClaims claims;

    /**
     * @param users user service
     * @param profiles profile repository
     * @param claims current-claims helper
     */
    public UsersController(UserService users, UserProfileRepository profiles, CurrentClaims claims) {
        this.users = users;
        this.profiles = profiles;
        this.claims = claims;
    }

    /**
     * The caller's own user view; 401 when no valid token is present.
     *
     * @param request the current request
     * @return the caller
     */
    @GetMapping("/me")
    public ResponseEntity<RestResponse<UserDto>> me(HttpServletRequest request) {
        UUID userId = claims.userId(request);
        if (userId == null) {
            return unauthorized();
        }
        return ResponseEntity.ok(RestResponse.ok(users.me(userId)));
    }

    /**
     * Admin: list users.
     *
     * @param pageable paging
     * @return matching page
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<Page<UserDto>>> list(
            @RequestParam(required = false) String query, Pageable pageable) {
        Page<UserDto> page = users.listUsers(pageable);
        return ResponseEntity.ok(RestResponse.ok(page));
    }

    /**
     * Patches a user's profile: admins can patch anyone, everyone else may
     * only patch themselves (ownership checked in code — the documented
     * IDOR mitigation).
     *
     * @param id target user
     * @param body patch payload (null fields are left unchanged)
     * @param request the current request
     * @return the updated profile view
     */
    @PatchMapping("/{id}/profile")
    @Transactional
    public ResponseEntity<RestResponse<UserDto>> updateProfile(@PathVariable UUID id,
                                                                @RequestBody ProfileUpdateRequest body,
                                                                HttpServletRequest request) {
        Claims c = claims.claims(request);
        if (c == null) {
            return unauthorized();
        }
        boolean isAdmin = c.roles() != null && c.roles().contains("ROLE_ADMIN");
        if (!isAdmin && !id.equals(c.userId())) {
            throw new ForbiddenException("you may only update your own profile");
        }
        UserProfile profile = profiles.findByUserId(id).orElseGet(() -> {
            UserProfile p = new UserProfile();
            p.setUserId(id);
            return p;
        });
        if (body.phone() != null) profile.setPhone(body.phone());
        if (body.dob() != null) profile.setDob(body.dob());
        if (body.gender() != null) profile.setGender(body.gender());
        if (body.preferredLanguage() != null) profile.setPreferredLanguage(body.preferredLanguage());
        if (body.preferredTimezone() != null) profile.setPreferredTimezone(body.preferredTimezone());
        if (body.bio() != null) profile.setBio(body.bio());
        if (body.notes() != null) profile.setNotes(body.notes());
        UserProfile saved = profiles.save(profile);
        log.info("Updated profile for user {} by {}", id, c.userId());
        UserDto target = users.me(id);
        return ResponseEntity.ok(RestResponse.ok(target));
    }

    private ResponseEntity<RestResponse<UserDto>> unauthorized() {
        return ResponseEntity.status(401).body(new RestResponse<>(
                false, null, new ErrorBody(ApiErrorCode.UNAUTHORIZED, "Authentication required"), null));
    }
}
