package com.company.account.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class CacheKeyGenerator {

    /**
     * 토큰을 SHA-256 해시로 변환 (보안을 위해 원본 토큰을 키로 사용하지 않음)
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * 사용자 ID 기반 캐시 키
     */
    public String userKey(Long userId) {
        return "user:" + userId;
    }

    /**
     * 이메일 기반 캐시 키
     */
    public String userEmailKey(String email) {
        return "user:email:" + email;
    }

    /**
     * Refresh Token 캐시 키
     */
    public String refreshTokenKey(Long userId) {
        return "auth:refresh:" + userId;
    }

    /**
     * Access Token 검증 결과 캐시 키
     */
    public String tokenValidationKey(String tokenHash) {
        return "auth:token:" + tokenHash;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.substring(0, Math.min(16, result.length())); // 해시 축약 (16자)
    }
}
