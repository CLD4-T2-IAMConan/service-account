package com.company.serviceaccount.domain;

import com.company.account.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * User 도메인 모델 테스트
 *
 * 테스트 범위:
 * - 사용자 생성 및 초기 상태
 * - 이메일 인증
 * - 이메일 형식 검증
 * - 도메인 로직
 */
@DisplayName("User 도메인 모델 테스트")
class UserTest {

    @Test
    @DisplayName("사용자 생성 - 초기 상태 검증")
    void createUser_initialState() {
        // Arrange & Act
        User user = User.builder()
            .email("test@example.com")
            .password("encodedPassword")
            .name("테스터")
            .build();

        // Assert
        assertThat(user.getEmailVerified()).isFalse();
        assertThat(user.getRole()).isEqualTo(User.UserRole.USER);
        assertThat(user.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("이메일 인증 완료")
    void verifyEmail_success() {
        // Arrange
        User user = User.builder()
            .email("test@example.com")
            .emailVerified(false)
            .build();

        // Act
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(java.time.LocalDateTime.now());

        // Assert
        assertThat(user.getEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 인증된 이메일 상태 확인")
    void verifyEmail_alreadyVerified() {
        // Arrange
        User user = User.builder()
            .email("test@example.com")
            .emailVerified(true)
            .emailVerifiedAt(java.time.LocalDateTime.now())
            .build();

        // Assert
        assertThat(user.getEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("사용자 상태 변경 - SUSPENDED")
    void changeStatus_toSuspended() {
        // Arrange
        User user = User.builder()
            .email("test@example.com")
            .status(User.UserStatus.ACTIVE)
            .build();

        // Act
        user.setStatus(User.UserStatus.SUSPENDED);

        // Assert
        assertThat(user.getStatus()).isEqualTo(User.UserStatus.SUSPENDED);
    }

    @Test
    @DisplayName("사용자 상태 변경 - ACTIVE")
    void changeStatus_toActive() {
        // Arrange
        User user = User.builder()
            .email("test@example.com")
            .status(User.UserStatus.SUSPENDED)
            .build();

        // Act
        user.setStatus(User.UserStatus.ACTIVE);

        // Assert
        assertThat(user.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("권한 변경 - ADMIN으로 승격")
    void changeRole_toAdmin() {
        // Arrange
        User user = User.builder()
            .email("test@example.com")
            .role(User.UserRole.USER)
            .build();

        // Act
        user.setRole(User.UserRole.ADMIN);

        // Assert
        assertThat(user.getRole()).isEqualTo(User.UserRole.ADMIN);
    }

    @Test
    @DisplayName("비밀번호 변경")
    void changePassword_success() {
        // Arrange
        User user = User.builder()
            .email("test@example.com")
            .password("oldPassword")
            .build();

        // Act
        user.setPassword("newEncodedPassword");

        // Assert
        assertThat(user.getPassword()).isEqualTo("newEncodedPassword");
    }

    @Test
    @DisplayName("프로필 업데이트")
    void updateProfile_success() {
        // Arrange
        User user = User.builder()
            .email("test@example.com")
            .name("Old Name")
            .build();

        // Act
        user.setName("New Name");
        user.setPhone("010-1234-5678");

        // Assert
        assertThat(user.getName()).isEqualTo("New Name");
        assertThat(user.getPhone()).isEqualTo("010-1234-5678");
    }
}
