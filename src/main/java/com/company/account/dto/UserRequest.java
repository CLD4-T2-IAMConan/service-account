package com.company.account.dto;

import com.company.account.entity.User.SocialProvider;
import com.company.account.entity.User.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserRequest {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "유효한 이메일 형식이어야 합니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
        private String password;

        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 50, message = "이름은 최대 50자까지 입니다")
        private String name;

        @Size(max = 50, message = "닉네임은 최대 50자까지 입니다")
        private String nickname;

        private String profileImageUrl;

        private SocialProvider provider;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {
        @Size(max = 50, message = "이름은 최대 50자까지 입니다")
        private String name;

        @Size(max = 50, message = "닉네임은 최대 50자까지 입니다")
        private String nickname;

        private String profileImageUrl;

        @Size(max = 20, message = "전화번호는 최대 20자까지 입니다")
        private String phone;

        // password는 null일 수 있으므로 @Size 대신 조건부 검증 사용
        // @Size는 null이 아닐 때만 검증하지만, 명시적으로 처리
        private String password;
        
        // password 검증을 위한 커스텀 메서드 (필요시 추가)
        public boolean hasPassword() {
            return password != null && !password.isEmpty();
        }
        
        public boolean isValidPassword() {
            return !hasPassword() || password.length() >= 8;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRole {
        private UserRole role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Login {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "유효한 이메일 형식이어야 합니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePassword {
        // 소셜 로그인 사용자의 경우 oldPassword는 선택사항
        private String oldPassword;

        @NotBlank(message = "새 비밀번호는 필수입니다")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
        private String newPassword;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyPassword {
        @NotBlank(message = "비밀번호는 필수입니다")
        private String password;
    }

    /**
     * 비밀번호 설정 (소셜 로그인 사용자용)
     * 기존 비밀번호 확인 없이 새 비밀번호만 설정
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SetPassword {
        @NotBlank(message = "새 비밀번호는 필수입니다")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
        private String newPassword;
    }
}
