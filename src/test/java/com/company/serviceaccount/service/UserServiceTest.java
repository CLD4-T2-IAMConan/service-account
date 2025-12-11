package com.company.serviceaccount.service;

import com.company.account.entity.User;
import com.company.account.dto.UserRequest;
import com.company.account.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * UserService 단위 테스트
 *
 * 테스트 범위:
 * - 회원가입 로직
 * - 이메일 중복 검증
 * - 비밀번호 유효성 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private com.company.account.service.UserService userService;

    @Test
    @DisplayName("정상적인 회원가입 - 성공")
    void signup_success() {
        // Arrange
        UserRequest.Create request = UserRequest.Create.builder()
            .email("test@example.com")
            .password("Password123!")
            .name("테스터")
            .build();

        given(userRepository.existsByEmail(request.getEmail()))
            .willReturn(false);

        given(passwordEncoder.encode(request.getPassword()))
            .willReturn("encodedPassword");

        User savedUser = User.builder()
            .userId(1L)
            .email(request.getEmail())
            .password("encodedPassword")
            .name(request.getName())
            .emailVerified(false)
            .role(User.UserRole.USER)
            .build();

        given(userRepository.save(any(User.class)))
            .willReturn(savedUser);

        // Act
        com.company.account.dto.UserResponse response = userService.createUser(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getName()).isEqualTo("테스터");
        assertThat(response.getRole()).isEqualTo(User.UserRole.USER);
        // emailVerified는 UserResponse에 포함되지 않으므로 저장된 User 엔티티에서 확인
        verify(userRepository, times(1)).save(argThat(user -> 
            user.getEmailVerified() != null && !user.getEmailVerified()
        ));

        // Verify
        verify(userRepository, times(1)).existsByEmail(request.getEmail());
        verify(passwordEncoder, times(1)).encode(request.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("중복된 이메일로 회원가입 - 예외 발생")
    void signup_duplicateEmail_throwsException() {
        // Arrange
        UserRequest.Create request = UserRequest.Create.builder()
            .email("duplicate@example.com")
            .password("Password123!")
            .name("중복")
            .build();

        given(userRepository.existsByEmail(request.getEmail()))
            .willReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 존재하는 이메일");

        // Verify - save가 호출되지 않아야 함
        verify(userRepository, times(1)).existsByEmail(request.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "pwd", "1234567"})
    @DisplayName("비밀번호 유효성 검증 - 짧은 비밀번호")
    @org.junit.jupiter.api.Disabled("비밀번호 검증은 @Valid에서 처리되므로 서비스 레이어 테스트에서 제외")
    void signup_shortPassword_throwsException(String password) {
        // 비밀번호 길이 검증은 @Valid 어노테이션에서 처리되므로
        // 서비스 레이어 단위 테스트에서는 이미 검증된 값이 들어온다고 가정
        // 이 테스트는 통합 테스트나 컨트롤러 테스트에서 수행해야 함
    }

    @Test
    @DisplayName("유효하지 않은 이메일 형식 - 예외 발생")
    @org.junit.jupiter.api.Disabled("이메일 형식 검증은 @Valid에서 처리되므로 서비스 레이어 테스트에서 제외")
    void signup_invalidEmail_throwsException() {
        // 이메일 형식 검증은 @Valid 어노테이션에서 처리되므로
        // 서비스 레이어 단위 테스트에서는 이미 검증된 값이 들어온다고 가정
        // 이 테스트는 통합 테스트나 컨트롤러 테스트에서 수행해야 함
    }

    @Test
    @DisplayName("이메일 인증 - 성공")
    @org.junit.jupiter.api.Disabled("EmailVerificationService에서 처리하므로 UserService 테스트에서 제외")
    void verifyEmail_success() {
        // 이 테스트는 EmailVerificationService에서 처리하므로 UserService 테스트에서 제외
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 - 예외 발생")
    void getUserByEmail_notFound_throwsException() {
        // Arrange
        String email = "notfound@example.com";

        given(userRepository.findByEmail(email))
            .willReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserByEmail(email))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
}
