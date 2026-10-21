package com.amrutam.common;

import com.amrutam.common.security.JwTokenService.Claims;
import com.amrutam.common.security.JwTokenService.Subject;
import com.amrutam.common.security.JwtException;
import com.amrutam.common.security.JwtProperties;
import com.amrutam.common.security.JwtTokenService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
// import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef";

    private JwtProperties props() {
        JwtProperties p = new JwtProperties();
        p.setSecret(SECRET);
        p.setIssuer("amrutam-test");
        p.setTtlSeconds(300);
        p.setClockSkewSeconds(30);
        return p;
    }

    private JwtTokenService service() {
        return new JwtTokenService(props(),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void issueAndParseRoundTrip() {
        JwtTokenService svc = service();
        UUID uid = UUID.randomUUID();
        String token = svc.issue(new Subject(uid, "alice", List.of("ROLE_PATIENT")), "jti-1", 300L);

        Claims claims = svc.parseAndValidate(token);
        assertThat(claims.userId()).isEqualTo(uid);
        assertThat(claims.roles()).containsExactly("ROLE_PATIENT");
        assertThat(claims.jwtId()).isEqualTo("jti-1");
        assertThat(svc.canParse(token)).isTrue();
    }

    @Test
    void expiredTokenThrows() {
        JwtTokenService svc = service();
        String token = svc.issue(new Subject(UUID.randomUUID(), "bob", List.of("ROLE_DOCTOR")),
                "jti-2", 60L);

        // Advance the clock past expiry + skew and re-validate.
        JwtTokenService later = new JwtTokenService(props(),
                Clock.fixed(Instant.parse("2026-01-01T01:00:00Z"), ZoneOffset.UTC));
        assertThatThrownBy(() -> later.parseAndValidate(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void wrongSecretFailsValidation() {
        JwtTokenService svc = service();
        String token = svc.issue(new Subject(UUID.randomUUID(), "carol", List.of("ROLE_ADMIN")),
                "jti-3", 300L);

        JwtProperties other = props();
        other.setSecret("ffffffffffffffffffffffffffffffffffffffff");
        JwtTokenService otherSvc = new JwtTokenService(other,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(() -> otherSvc.parseAndValidate(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void legacySecretStillValidDuringRotation() {
        JwtProperties original = props();
        JwtProperties rotated = props();
        rotated.setSecret("fedcba9876543210fedcba9876543210fedcba9876543210");
        rotated.setLegacySecret(original.getSecret());

        String token = new JwtTokenService(original,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC))
                .issue(new Subject(UUID.randomUUID(), "dave", List.of("ROLE_PATIENT")),
                        "jti-legacy", 300L);

        Claims claims = new JwtTokenService(rotated,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC))
                .parseAndValidate(token);
        assertThat(claims.userId()).isNotNull();
        assertThat(claims.roles()).containsExactly("ROLE_PATIENT");
    }
}
