package com.company.account.service;

import com.company.account.entity.EmailVerification;
import com.company.account.entity.User;
import com.company.account.repository.EmailVerificationRepository;
import com.company.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 10;

    /**
     * 이메일 인증 코드 생성 및 전송
     */
    @Transactional
    public void sendVerificationCode(String email) {
        log.info("Sending verification code to email: {}", email);

        // 6자리 랜덤 숫자 생성
        String verificationCode = generateVerificationCode();

        // 인증 코드 저장
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .verificationCode(verificationCode)
                .expiresAt(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES))
                .verified(false)
                .build();

        emailVerificationRepository.save(verification);

        // 이메일 전송
        emailService.sendVerificationEmail(email, verificationCode);

        log.info("Verification code sent successfully to: {}", email);
    }

    /**
     * 이메일 인증 코드 검증
     */
    @Transactional
    public boolean verifyCode(String email, String code) {
        log.info("Verifying code for email: {}", email);

        EmailVerification verification = emailVerificationRepository
                .findByEmailAndVerificationCodeAndVerifiedFalse(email, code)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 인증 코드입니다"));

        if (verification.isExpired()) {
            throw new IllegalArgumentException("인증 코드가 만료되었습니다");
        }

        // 인증 완료 처리
        verification.setVerified(true);
        verification.setVerifiedAt(LocalDateTime.now());
        emailVerificationRepository.save(verification);

        // 사용자의 이메일 인증 상태 업데이트
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("User email verified: {}", email);

            // 환영 이메일 전송
            emailService.sendWelcomeEmail(email, user.getName());
        });

        log.info("Email verification successful for: {}", email);
        return true;
    }

    /**
     * 6자리 랜덤 숫자 생성
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 만료된 인증 코드 정리 (스케줄러로 실행 가능)
     */
    @Transactional
    public void cleanupExpiredCodes() {
        emailVerificationRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Expired verification codes cleaned up");
    }
}
