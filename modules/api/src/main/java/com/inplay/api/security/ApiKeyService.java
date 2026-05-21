package com.inplay.api.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * API key 발급·해시·검증.
 *
 * <p>발급: 32 byte random → base64 url-safe (~43 char). 사용자에게 한 번만 노출. 서버 저장 X.
 * 저장: sha256(raw) hex 64-char (User.apiKeyHash).
 * 검증: 상수시간(constant-time) 비교로 timing attack 차단.
 */
public final class ApiKeyService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public String generateRawKey() {
        byte[] buf = new byte[32];
        RNG.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    public String hash(String rawKey) {
        Objects.requireNonNull(rawKey, "rawKey required");
        if (rawKey.isBlank()) {
            throw new IllegalArgumentException("rawKey must not be blank");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public boolean verify(String rawKey, String storedHash) {
        if (rawKey == null || storedHash == null) {
            return false;
        }
        if (storedHash.length() != 64) {
            return false;
        }
        String computed = hash(rawKey);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.US_ASCII),
                storedHash.getBytes(StandardCharsets.US_ASCII));
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }
}
