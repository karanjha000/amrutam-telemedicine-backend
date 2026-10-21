package com.amrutam.api.controller;

import com.amrutam.api.dto.LoginRequest;
import com.amrutam.api.dto.LoginResponse;
import com.amrutam.api.dto.MfaVerifyRequest;
import com.amrutam.api.dto.PatientDto;
import com.amrutam.api.dto.RefreshRequest;
import com.amrutam.api.dto.RegisterPatientRequest;
import com.amrutam.api.dto.UserDto;
import com.amrutam.api.service.UserService;
import com.amrutam.api.util.CurrentClaims;
import com.amrutam.common.exception.ApiErrorCode;
import com.amrutam.common.exception.ErrorBody;
import com.amrutam.common.exception.RestResponse;
import com.amrutam.common.security.AuthHeaderFilter;
import com.amrutam.common.security.JwTokenService.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth endpoints: registration, login, MFA verify, refresh, me.
 *
 * <p>The JWT bearer token is validated by the common kernel's
 * {@link AuthHeaderFilter}, which publishes the parsed {@link Claims} as a
 * request attribute; {@code /me} reads that attribute via
 * {@link CurrentClaims} and 401s when no valid token was presented.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService users;
    private final CurrentClaims claims;

    /**
     * @param users user service
     * @param claims current-claims helper
     */
    public AuthController(UserService users, CurrentClaims claims) {
        this.users = users;
        this.claims = claims;
    }

    /**
     * Registers a new patient.
     *
     * @param request registration payload
     * @return the created patient
     */
    @PostMapping("/register")
    public ResponseEntity<RestResponse<PatientDto>> register(@Valid @RequestBody RegisterPatientRequest request) {
        return ResponseEntity.ok(RestResponse.ok(users.registerPatient(request)));
    }

    /**
     * Logs in; returns a token, or an MFA challenge when TOTP is enabled.
     *
     * @param request login payload
     * @return the login response
     */
    @PostMapping("/login")
    public ResponseEntity<RestResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(RestResponse.ok(users.login(request)));
    }

    /**
     * Verifies an MFA code and issues the token (stub TOTP).
     *
     * @param request mfa verify payload
     * @return the issued access token
     */
    @PostMapping("/mfa/verify")
    public ResponseEntity<RestResponse<String>> mfaVerify(@Valid @RequestBody MfaVerifyRequest request) {
        return ResponseEntity.ok(RestResponse.ok(users.verifyMfa(request)));
    }

    /**
     * Re-issues a token for a still-valid one.
     *
     * @param request refresh payload
     * @return the refreshed access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<RestResponse<String>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(RestResponse.ok(users.refresh(request.token())));
    }

    /**
     * Returns the caller's own user view; 401 when no valid token is present.
     *
     * @param request the current request (claims request attribute)
     * @return the user
     */
    @GetMapping("/me")
    public ResponseEntity<RestResponse<UserDto>> me(HttpServletRequest request) {
        Claims c = claims.claims(request);
        if (c == null) {
            return unauthorized();
        }
        return ResponseEntity.ok(RestResponse.ok(users.me(c.userId())));
    }

    private ResponseEntity<RestResponse<UserDto>> unauthorized() {
        return ResponseEntity.status(401).body(new RestResponse<>(
                false, null, new ErrorBody(ApiErrorCode.UNAUTHORIZED, "Authentication required"), null));
    }
}
