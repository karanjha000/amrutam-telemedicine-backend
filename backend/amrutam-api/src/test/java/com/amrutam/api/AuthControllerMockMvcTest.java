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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Locale;
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

    @Test
    void mfaVerifyAcceptsOnlyValidTotp() throws Exception {
        com.amrutam.api.entity.User user = new com.amrutam.api.entity.User();
        user.setId(UUID.randomUUID());
        user.setUsername("mfa-verify");
        user.setPasswordHash(new BCryptPasswordEncoder().encode("pw"));
        user.setRole(com.amrutam.api.entity.User.RoleString.PATIENT);
        user.setMfaEnabled(true);
        user.setMfaSecret("JBSWY3DPEHPK3PXP");
        user.setActive(true);
        when(users.findByUsername("mfa-verify")).thenReturn(Optional.of(user));
        when(jwTokenService.issue(any(JwTokenService.Subject.class), anyString(), anyLong()))
                .thenReturn("mfa-token");

        String validCode = generateTotp(user.getMfaSecret());
        String invalidCode = "000000";

        mockMvc.perform(post("/api/v1/auth/mfa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"mfa-verify\",\"code\":\"" + validCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("mfa-token"));

        mockMvc.perform(post("/api/v1/auth/mfa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"mfa-verify\",\"code\":\"" + invalidCode + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    private static String generateTotp(String base32Secret) {
        try {
            byte[] key = decodeBase32(base32Secret);
            long timeCounter = System.currentTimeMillis() / 1000L / 30L;
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.putLong(timeCounter);
            byte[] data = buffer.array();
            SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % 1_000_000;
            return String.format(Locale.ROOT, "%06d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate TOTP", e);
        }
    }

    private static byte[] decodeBase32(String base32) {
        String cleaned = base32.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        int value = 0;
        int bits = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (char ch : cleaned.toCharArray()) {
            if (ch >= 'A' && ch <= 'Z') {
                value = (value << 5) | (ch - 'A');
            } else if (ch >= '2' && ch <= '7') {
                value = (value << 5) | (ch - '2' + 26);
            } else {
                continue;
            }
            bits += 5;
            while (bits >= 8) {
                bits -= 8;
                out.write((value >> bits) & 0xFF);
            }
        }
        return out.toByteArray();
    }
}
