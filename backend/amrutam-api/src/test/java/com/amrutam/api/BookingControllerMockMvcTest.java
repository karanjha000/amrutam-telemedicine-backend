package com.amrutam.api;

import com.amrutam.api.controller.ConsultationsController;
import com.amrutam.api.dto.BookConsultationResponse;
import com.amrutam.api.exception.GlobalExceptionHandler;
import com.amrutam.api.repository.UserRepository;
import com.amrutam.api.security.ApiSecurityConfig;
import com.amrutam.api.service.BookingService;
import com.amrutam.api.service.PaymentService;
import com.amrutam.api.util.CurrentClaims;
import com.amrutam.common.config.CommonAutoConfiguration;
import com.amrutam.common.security.AuthHeaderFilter;
import com.amrutam.common.security.JwTokenService.Claims;
import com.amrutam.common.security.JwTokenService;
import com.amrutam.common.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests of {@link ConsultationsController} with a mocked
 * {@link BookingService}. Asserts that the {@code X-Idempotency-Key} header
 * is passed through to the service and that a non-replayed result yields a
 * 201.
 *
 * <p>Authentication is faked at the {@link CurrentClaims} seam (not the real
 * JWT filter chain): a {@link RequestPostProcessor} stamps a
 * {@link Claims} request attribute, which is exactly what the production
 * {@code AuthHeaderFilter} does when a valid Bearer token is present.</p>
 */
@WebMvcTest(controllers = {ConsultationsController.class})
@ActiveProfiles("test")
@Import({SecurityConfig.class,
        ApiSecurityConfig.class,
        CommonAutoConfiguration.class,
        GlobalExceptionHandler.class,
        CurrentClaims.class})
class BookingControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private com.amrutam.api.repository.ConsultationRepository consultationRepository;

    @MockBean
    private JwTokenService jwTokenService;

    @MockBean
    private UserRepository userRepository;

    private static final UUID DOCTOR = UUID.randomUUID();
    private static final UUID SLOT = UUID.randomUUID();
    private static final UUID CONSULTATION = UUID.randomUUID();
    private static final UUID PAYMENT = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();

    /** Stamps a Claims attribute on the request, mirroring AuthHeaderFilter. */
    private static RequestPostProcessor authenticated() {
        Claims claims = new Claims(PATIENT, List.of("ROLE_PATIENT"), "jti-1", 0L, 9_999_999_999L);
        UsernamePasswordAuthenticationToken auth =
                UsernamePasswordAuthenticationToken.authenticated(
                        String.valueOf(PATIENT), null,
                        List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));
        RequestPostProcessor securityAuth = SecurityMockMvcRequestPostProcessors.authentication(auth);

        return request -> {
            request.setAttribute(AuthHeaderFilter.AUTH_CLAIMS_ATTR, claims);
            request.setAttribute(AuthHeaderFilter.AUTH_CLAIMS_ALIASED_ATTR, claims);
            SecurityContextHolder.getContext().setAuthentication(auth);
            return securityAuth.postProcessRequest(request);
        };
    }

    @Test
    void bookingUnauthenticatedIs404WhenNoClaims() throws Exception {
        // No Claims attribute: the controller surfaces a 401 via
        // JwtException.unauthorized (mapped by GlobalExceptionHandler).
        mockMvc.perform(post("/api/v1/consultations/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void bookingWithExplicitKeyPassesItToService() throws Exception {
        when(bookingService.book(any(com.amrutam.api.dto.BookingRequest.class),
                        any(UUID.class), any()))
                .thenReturn(new BookConsultationResponse(CONSULTATION, SLOT, PAYMENT, "PAID", false));

        String key = "explicit-key-1";
        ResultActions res = mockMvc.perform(post("/api/v1/consultations/booking")
                        .with(authenticated())
                        .header("X-Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody()));
        res.andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.replayed").value(false));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(bookingService).book(any(com.amrutam.api.dto.BookingRequest.class),
                any(UUID.class), keyCaptor.capture());
        assertThat(keyCaptor.getValue()).isEqualTo(key);
    }

    @Test
    void replayedResultReturns200() throws Exception {
        when(bookingService.book(any(com.amrutam.api.dto.BookingRequest.class),
                        any(UUID.class), any()))
                .thenReturn(new BookConsultationResponse(CONSULTATION, SLOT, PAYMENT, "PAID", true));

        mockMvc.perform(post("/api/v1/consultations/booking")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.replayed").value(true));
    }

    @Test
    void missingRequiredBodyFieldIs400() throws Exception {
        String body = """
                {"doctorId":null,"slotId":"%s"}
                """.formatted(SLOT);
        mockMvc.perform(post("/api/v1/consultations/booking")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private static String jsonBody() {
        return """
                {"doctorId":"%s","slotId":"%s","paymentMethod":"card","paymentToken":"tok"}
                """.formatted(DOCTOR, SLOT);
    }
}
