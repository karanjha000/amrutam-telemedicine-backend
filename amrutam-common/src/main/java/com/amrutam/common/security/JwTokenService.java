package com.amrutam.common.security;

import java.util.List;
import java.util.UUID;

/**
 * Abstraction over JWT issuance and validation so that tests (and, if we
 * ever swap the token provider) can substitute implementations freely.
 */
public interface JwTokenService {

    /**
     * Subject that gets encoded into the issued token.
     *
     * @param userId   the user id
     * @param username the user name (subject claim)
     * @param roles    the authority strings
     */
    record Subject(UUID userId, String username, List<String> roles) {
    }

    /**
     * Issues a signed token for the given subject.
     *
     * @param subject the subject
     * @param jti     the unique token id
     * @param ttlSeconds time-to-live for this token
     * @return the encoded JWT
     */
    String issue(Subject subject, String jti, long ttlSeconds);

    /**
     * Parses and cryptographically validates a token, returning its claims.
     *
     * @param token the encoded JWT
     * @return the verified claims
     * @throws JwtException if the signature, expiry or issuer is invalid
     */
    Claims parseAndValidate(String token);

    /**
     * Checks whether the token decodes into a readable structure; does not
     * imply that validation will succeed.
     *
     * @param token the encoded JWT
     * @return true when decodable
     */
    boolean canParse(String token);
}
