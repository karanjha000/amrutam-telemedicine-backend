package com.amrutam.api;

import com.amrutam.api.controller.AuthController;
import com.amrutam.api.exception.GlobalExceptionHandler;
import com.amrutam.api.repository.UserProfileRepository;
import com.amrutam.api.repository.UserRepository;
import com.amrutam.api.security.ApiSecurityConfig;
import com.amrutam.api.service.impl.UserServiceImpl;
import com.amrutam.api.util.CurrentClaims;
import com.amrutam.common.config.CommonAutoConfiguration;
import com.amrutam.common.security.JwTokenService;
import com.amrutam.common.security.JwtProperties;
import com.amrutam.common.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests of {@link AuthController} with the real security chain
 * (via {@link ApiSecurityConfig} + {@link CommonAutoConfiguration}) and a
 * mocked {@link JwTokenService}.
 */
@WebMvcTest(controllers = {AuthController.class})
@ActiveProfiles("test")
@Import({SecurityConfig.class,
        ApiSecurityConfig.class,
        CommonAutoConfiguration.class,
        GlobalExceptionHandler.class,
        CurrentClaims.class,
        UserServiceImpl.class})
class AuthControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository users;

    @MockBean
    private UserProfileRepository profiles;

    @MockBean
    private JwTokenService jwTokenService;

    @MockBean
    private JwtProperties jwtProperties;

    @MockBean
    private com.amrutam.api.audit.AuditRecorder auditRecorder;

    @Test
    void registerValidBody() throws Exception {
        when(users.findByUsername("newuser")).thenReturn(Optional.empty());
        when(users.findByUsername("newuser".toLowerCase())).thenReturn(Optional.empty());

        String body = """
                {"username":"newuser","password":"secret123","email":"n@x.com","displayName":"New User","phone":"123"}
                """;
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("newuser"));
    }

    @Test
    void registerDuplicateUsername() throws Exception {
        com.amrutam.api.entity.User existing = new com.amrutam.api.entity.User();
        existing.setId(UUID.randomUUID());
        existing.setUsername("dupuser");
        when(users.findByUsername("dupuser")).thenReturn(Optional.of(existing));

        String body = """
                {"username":"dupuser","password":"secret123","email":"d@x.com","displayName":"Dup User","phone":"123"}
                """;
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void loginMfaEnabledReturnsChallenge() throws Exception {
        com.amrutam.api.entity.User user = new com.amrutam.api.entity.User();
        user.setId(UUID.randomUUID());
        user.setUsername("mfauser");
        user.setPasswordHash(new BCryptPasswordEncoder().encode("pw"));
        user.setRole(com.amrutam.api.entity.User.RoleString.PATIENT);
        user.setMfaEnabled(true);
        user.setMfaSecret("SECRET");
        user.setActive(true);
        when(users.findByUsername("mfauser")).thenReturn(Optional.of(user));

        String body = """
                {"username":"mfauser","password":"pw"}
                """;
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isEmpty())
                .andExpect(jsonPath("$.data.mfaChallenge.secret").value("SECRET"));
    }

    @Test
    void loginNoMfaReturnsToken() throws Exception {
        com.amrutam.api.entity.User user = new com.amrutam.api.entity.User();
        user.setId(UUID.randomUUID());
        user.setUsername("nomfa");
        user.setPasswordHash(new BCryptPasswordEncoder().encode("pw"));
        user.setRole(com.amrutam.api.entity.User.RoleString.PATIENT);
        user.setMfaEnabled(false);
        user.setActive(true);
        when(users.findByUsername("nomfa")).thenReturn(Optional.of(user));
        when(jwTokenService.issue(any(JwTokenService.Subject.class), anyString(), anyLong()))
                .thenReturn("fake-jwt");

        String body = """
                {"username":"nomfa","password":"pw"}
                """;
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("fake-jwt"));
    }
}
