package com.amrutam.api.service;

import com.amrutam.api.dto.LoginRequest;
import com.amrutam.api.dto.LoginResponse;
import com.amrutam.api.dto.MfaVerifyRequest;
import com.amrutam.api.dto.PatientDto;
import com.amrutam.api.dto.RegisterPatientRequest;
import com.amrutam.api.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Auth / user-facing service.
 */
public interface UserService {

    /**
     * Registers a new patient account (409 on username clash).
     *
     * @param request registration payload
     * @return the created patient
     */
    PatientDto registerPatient(RegisterPatientRequest request);

    /**
     * Logs in; returns a token for non-MFA users and an MFA challenge for
     * MFA-enabled users (401 on bad credentials or a disabled account).
     *
     * @param request login payload
     * @return the login response
     */
    LoginResponse login(LoginRequest request);

    /**
     * Verifies an MFA code and issues the token. Stub: accepts any
     * well-formed 6-digit code while the TOTP integration is pending.
     *
     * @param request mfa verify payload
     * @return the issued access token
     */
    String verifyMfa(MfaVerifyRequest request);

    /**
     * Re-issues a token for a still-valid one (fresh {@code jti}).
     *
     * @param token current token
     * @return the refreshed access token
     */
    String refresh(String token);

    /**
     * Returns the caller's own user view, including profile-derived fields.
     *
     * @param userId the acting user
     * @return the user
     */
    UserDto me(UUID userId);

    /**
     * Admin directory: list / search users.
     *
     * @param pageable paging
     * @return matching page
     */
    Page<UserDto> listUsers(Pageable pageable);
}
