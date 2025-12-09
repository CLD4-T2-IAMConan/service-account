package com.company.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthRequest {

    /**
     * 회원가입 요청
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignUp {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
        private String password;

        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다")
        private String name;

        @Size(max = 50, message = "닉네임은 50자를 초과할 수 없습니다")
        private String nickname;
    }

    /**
     * 이메일 인증 코드 전송 요청
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendVerificationCode {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;
    }

    /**
     * 이메일 인증 코드 검증 요청
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyEmail {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "인증 코드는 필수입니다")
        @Size(min = 6, max = 6, message = "인증 코드는 6자리입니다")
        private String code;
    }

    /**
     * 로그인 요청
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Login {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        private String password;
    }

    /**
     * 토큰 갱신 요청
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshToken {
        @NotBlank(message = "Refresh Token은 필수입니다")
        private String refreshToken;
    }
}
