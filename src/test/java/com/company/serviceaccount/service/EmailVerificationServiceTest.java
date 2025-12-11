package com.company.serviceaccount.service;

import com.company.account.entity.EmailVerification;
import com.company.account.entity.User;
import com.company.account.repository.EmailVerificationRepository;
import com.company.account.repository.UserRepository;
import com.company.account.service.EmailService;
import com.company.account.service.EmailVerificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

/**
 * EmailVerificationService 단위 테스트
 *
 * 테스트 범위:
 * - 이메일 인증 코드 생성 및 전송
 * - 이메일 인증 코드 검증
 * - 만료된 인증 코드 정리
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService 단위 테스트")
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    @DisplayName("이메일 인증 코드 생성 및 전송 - 성공")
    void sendVerificationCode_success() {
        // Arrange
        String email = "test@example.com";
        EmailVerification savedVerification = EmailVerification.builder()
            .verificationId(1L)
            .email(email)
            .verificationCode("123456")
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .verified(false)
            .build();

        given(emailVerificationRepository.save(any(EmailVerification.class)))
            .willReturn(savedVerification);
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        // Act
        emailVerificationService.sendVerificationCode(email);

        // Assert
        ArgumentCaptor<EmailVerification> verificationCaptor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(emailVerificationRepository, times(1)).save(verificationCaptor.capture());

        EmailVerification captured = verificationCaptor.getValue();
        assertThat(captured.getEmail()).isEqualTo(email);
        assertThat(captured.getVerificationCode()).isNotNull();
        assertThat(captured.getVerificationCode().length()).isEqualTo(6); // 6자리 숫자
        assertThat(captured.getVerified()).isFalse();
        assertThat(captured.getExpiresAt()).isAfter(LocalDateTime.now());

        verify(emailService, times(1)).sendVerificationEmail(eq(email), anyString());
    }

    @Test
    @DisplayName("이메일 인증 코드 검증 - 성공")
    void verifyCode_success() {
        // Arrange
        String email = "test@example.com";
        String code = "123456";
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        EmailVerification verification = EmailVerification.builder()
            .verificationId(1L)
            .email(email)
            .verificationCode(code)
            .expiresAt(expiresAt)
            .verified(false)
            .build();

        User user = User.builder()
            .userId(1L)
            .email(email)
            .name("테스터")
            .emailVerified(false)
            .build();

        given(emailVerificationRepository.findByEmailAndVerificationCodeAndVerifiedFalse(email, code))
            .willReturn(Optional.of(verification));
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willReturn(user);
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString());

        // Act
        boolean result = emailVerificationService.verifyCode(email, code);

        // Assert
        assertThat(result).isTrue();
        assertThat(verification.getVerified()).isTrue();
        assertThat(verification.getVerifiedAt()).isNotNull();
        assertThat(user.getEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isNotNull();

        verify(emailVerificationRepository, times(1))
            .findByEmailAndVerificationCodeAndVerifiedFalse(email, code);
        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendWelcomeEmail(eq(email), eq(user.getName()));
    }

    @Test
    @DisplayName("이메일 인증 코드 검증 - 유효하지 않은 코드로 인한 실패")
    void verifyCode_invalidCode_throwsException() {
        // Arrange
        String email = "test@example.com";
        String invalidCode = "999999";

        given(emailVerificationRepository.findByEmailAndVerificationCodeAndVerifiedFalse(email, invalidCode))
            .willReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> emailVerificationService.verifyCode(email, invalidCode))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("유효하지 않은 인증 코드");

        verify(emailVerificationRepository, times(1))
            .findByEmailAndVerificationCodeAndVerifiedFalse(email, invalidCode);
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("이메일 인증 코드 검증 - 만료된 코드로 인한 실패")
    void verifyCode_expiredCode_throwsException() {
        // Arrange
        String email = "test@example.com";
        String code = "123456";
        LocalDateTime pastExpiresAt = LocalDateTime.now().minusMinutes(1); // 이미 만료됨

        EmailVerification expiredVerification = EmailVerification.builder()
            .verificationId(1L)
            .email(email)
            .verificationCode(code)
            .expiresAt(pastExpiresAt)
            .verified(false)
            .build();

        given(emailVerificationRepository.findByEmailAndVerificationCodeAndVerifiedFalse(email, code))
            .willReturn(Optional.of(expiredVerification));

        // Act & Assert
        assertThatThrownBy(() -> emailVerificationService.verifyCode(email, code))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("인증 코드가 만료되었습니다");

        verify(emailVerificationRepository, times(1))
            .findByEmailAndVerificationCodeAndVerifiedFalse(email, code);
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("이메일 인증 코드 검증 - 이미 인증된 코드는 재사용 불가")
    void verifyCode_alreadyVerifiedCode_throwsException() {
        // Arrange
        String email = "test@example.com";
        String code = "123456";

        // 이미 인증된 코드는 findByEmailAndVerificationCodeAndVerifiedFalse에서 조회되지 않음
        given(emailVerificationRepository.findByEmailAndVerificationCodeAndVerifiedFalse(email, code))
            .willReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> emailVerificationService.verifyCode(email, code))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("유효하지 않은 인증 코드");

        verify(emailVerificationRepository, times(1))
            .findByEmailAndVerificationCodeAndVerifiedFalse(email, code);
    }

    @Test
    @DisplayName("이메일 인증 코드 검증 - 사용자가 존재하지 않는 경우")
    void verifyCode_userNotFound_stillSucceeds() {
        // Arrange
        String email = "test@example.com";
        String code = "123456";
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        EmailVerification verification = EmailVerification.builder()
            .verificationId(1L)
            .email(email)
            .verificationCode(code)
            .expiresAt(expiresAt)
            .verified(false)
            .build();

        given(emailVerificationRepository.findByEmailAndVerificationCodeAndVerifiedFalse(email, code))
            .willReturn(Optional.of(verification));
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // Act
        boolean result = emailVerificationService.verifyCode(email, code);

        // Assert
        assertThat(result).isTrue();
        assertThat(verification.getVerified()).isTrue();

        verify(emailVerificationRepository, times(1))
            .findByEmailAndVerificationCodeAndVerifiedFalse(email, code);
        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("만료된 인증 코드 정리 - 성공")
    void cleanupExpiredCodes_success() {
        // Arrange
        doNothing().when(emailVerificationRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));

        // Act
        emailVerificationService.cleanupExpiredCodes();

        // Assert
        ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(emailVerificationRepository, times(1)).deleteByExpiresAtBefore(dateCaptor.capture());

        LocalDateTime capturedDate = dateCaptor.getValue();
        // 약간의 시간 차이를 고려하여 현재 시간 이하인지 확인
        assertThat(capturedDate).isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("인증 코드는 6자리 숫자여야 함")
    void verificationCode_isSixDigits() {
        // Arrange
        String email = "test@example.com";
        EmailVerification savedVerification = EmailVerification.builder()
            .verificationId(1L)
            .email(email)
            .verificationCode("123456")
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .verified(false)
            .build();

        given(emailVerificationRepository.save(any(EmailVerification.class)))
            .willReturn(savedVerification);
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        // Act
        emailVerificationService.sendVerificationCode(email);

        // Assert
        ArgumentCaptor<EmailVerification> verificationCaptor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(emailVerificationRepository, times(1)).save(verificationCaptor.capture());

        String code = verificationCaptor.getValue().getVerificationCode();
        assertThat(code).hasSize(6);
        assertThat(code).matches("\\d{6}"); // 6자리 숫자만 허용
    }
}
