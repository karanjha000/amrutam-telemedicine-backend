# Amrutam Security

Threat model, OWASP Top-10 mapping, key rotation, and MFA notes.

## 1. Threat model

| Asset | Threat | Mitigation |
| --- | --- | --- |
| Patient data (consultations, prescriptions, payments) | Unauthenticated access | JWT bearer auth (15-min TTL); /api/v1/** requires a valid token; /me 401s without one |
|  | IDOR (reading another user's consultation) | @PreAuthorize role checks + ownership checks in services (ConsultationsController/BookingService verify patientId/doctorId against the validated claim userId, never a spoofable principal name) |
|  | Duplicate billing | Idempotency: (userId, method, path, bodyHash) natural key; payments.idempotency_key unique; an explicit X-Idempotency-Key header replaces the body hash so replays are stable |
| Doctor availability | Double-booking a slot | Atomic UPDATE ... WHERE status='AVAILABLE' (exactly one winner); 409 on race; 15-min lock window; workers' BookingSagaOrchestrator reconciles stuck PENDING_PAYMENT rows |
| Platform (admin analytics) | Privilege escalation | ROLE_ADMIN-gated @PreAuthorize("hasRole('ADMIN')") on /api/v1/admin/**; separate OpenAPI "admin" group |
| Secrets (JWT HMAC secret) | Secret reuse / leakage | Dev placeholder only; prod requires JWT_SECRET env var; two-secret overlap rotation (below); 15-min TTL limits blast radius |
| Integrity of audit trail | Tampering | audit_logs written via dedicated AUDIT logback appender (rolling file, 30-day retention) and by the workers' AuditWorker draining the Redis audit stream |

## 2. OWASP Top-10 mapping

| Category | Mitigation in code |
| --- | --- |
| A01 Broken Access Control | @PreAuthorize method security (common SecurityConfig enables it); service-level ownership checks for IDOR; admin endpoints role-gated |
| A02 Cryptographic Failures | BCrypt for password hashes; HMAC-SHA256 JWT (auth0 java-jwt); AES note for future at-rest field encryption; no plaintext PII in logs (audit details are operator-controlled) |
| A03 Injection | All persistence via JPA parameterized queries / @Query named params; workers use JdbcTemplate with bind variables; no string-built SQL |
| A04 Insecure Design | Booking saga with explicit compensation matrix (docs/ARCHITECTURE.md §4.2); exactly-once slot lock; idempotency key semantics documented |
| A05 Security Misconfiguration | Stateless JWT (no session fixation); CSRF disabled deliberately (stateless bearer auth); prod profile: forward-headers-strategy framework, minimal actuator exposure (health+prometheus), logging INFO |
| A06 Vulnerable Components | OWASP dependency-check in CI (security-scan job); dependency versions pinned in the parent BOM; quarterly re-scan |
| A07 Auth Failures | JWT 15-min TTL + refresh endpoint; MFA (TOTP) first-class in the login flow; disabled accounts rejected |
| A08 Software & Data Integrity | Idempotency + saga_steps trace; workers reconcile stuck sagas |
| A09 Logging Failures | MDC traceId on every response envelope + log line; AUDIT appender to a rolling file; audit_logs table for durable trail |
| A10 SSRF / XXE | No user-controlled outbound URLs in this scope; JSON-only API (XSS not applicable to a JSON API; no HTML rendering) |

Additional:
- DoS: RateLimitFilter (common kernel) per-endpoint token bucket
  (amrutam.ratelimit.default-qps/burst + overrides).
- Sensitive data exposure: passwords BCrypt-hashed; gateway tokens held only
  in memory during the request; payment idempotency keys are opaque.

## 3. Encryption at rest / in transit

- In transit: TLS termination at the platform proxy; prod sets
  server.forward-headers-strategy: framework so X-Forwarded-* is honored.
- At rest: PostgreSQL connection string should use sslmode=require in prod
  (documented, not enforced by this module yet). Redis should run behind
  TLS in prod.

## 4. Key rotation (JWT HMAC secret)

- Rotation is a two-secret overlap: keep the current secret in
  amrutam.jwt.secret and the previous one in amrutam.jwt.legacy-secret
  (documented convention; the verifier checks both during the overlap
  window). Re-issue strategy: existing tokens remain valid until they
  expire (15-min TTL bounds the window); new logins use the new secret.
- 15-min TTL + the refresh endpoint keep the rotation window short.

## 5. MFA / TOTP

- Login flow: when users.mfa_enabled is TRUE, /auth/login returns an
  MfaChallengeDto (secret + otpauth:// provisioning URI) instead of a
  token; /auth/mfa/verify then issues the token.
- Status: the TOTP verification is a STUB (accepts any well-formed 6-digit
  code) until the RFC 6238 integration lands; the secret column and
  provisioning URI are already wired. Track as a follow-up.
