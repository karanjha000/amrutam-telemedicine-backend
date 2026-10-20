package com.amrutam.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 hex helpers.
 */
public final class ShaUtil {

    private ShaUtil() {
    }

    /**
     * SHA-256 hex digest of a UTF-8 string.
     *
     * @param s the input
     * @return the hex digest (64 chars, lowercase)
     */
    public static String sha256Hex(String s) {
        return sha256Hex(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * SHA-256 hex digest of a byte array.
     *
     * @param b the input
     * @return the hex digest (lowercase)
     */
    public static String sha256Hex(byte[] b) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(b);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte x : hash) {
                sb.append(Character.forDigit((x >> 4) & 0xF, 16));
                sb.append(Character.forDigit(x & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by every JRE; this should never happen.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
