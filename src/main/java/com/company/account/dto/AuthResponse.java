package com.company.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class AuthResponse {

    /**
     * 로그인 응답 (토큰 포함)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {
        private Long userId;
        private String email;
        private String name;
        private String role;
        private String accessToken;
        private String refreshToken;
        private LocalDateTime expiresAt;
    }

    /**
     * 토큰 갱신 응답
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private LocalDateTime expiresAt;
    }

    /**
     * 이메일 인증 코드 전송 응답
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificationCodeSent {
        private String email;
        private LocalDateTime expiresAt;
    }
}
