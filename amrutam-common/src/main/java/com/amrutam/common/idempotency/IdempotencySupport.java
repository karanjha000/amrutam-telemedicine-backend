package com.amrutam.common.idempotency;

import com.amrutam.common.util.ShaUtil;

import java.nio.charset.StandardCharsets;

/**
 * Static helpers for building idempotency keys.
 */
public final class IdempotencySupport {

    private IdempotencySupport() {
    }

    /**
     * Computes a SHA-256 hex digest of a body, salted per user, so two
     * different bodies that hash to the same value without a salt cannot
     * collide across users.
     *
     * @param body the request body (empty string when no body)
     * @param salt the salt (typically the user id)
     * @return the salted hash
     */
    public static String bodyHash(String body, String salt) {
        String safeBody = body == null ? "" : body;
        String safeSalt = salt == null ? "" : salt;
        return ShaUtil.sha256Hex(
                (safeSalt + ":" + safeBody).getBytes(StandardCharsets.UTF_8));
    }
}
