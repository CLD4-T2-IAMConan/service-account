package com.company.account.controller;

import com.company.account.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 이메일 전송 테스트 컨트롤러
 * 개발 및 테스트 환경에서만 활성화
 */
@Slf4j
@RestController
@RequestMapping("/api/test/email")
@RequiredArgsConstructor
@Profile({"dev", "ses", "smtp"})
public class EmailTestController {

    private final EmailService emailService;

    /**
     * 인증 이메일 테스트
     *
     * @param email 수신자 이메일
     * @return 성공 메시지
     */
    @PostMapping("/verification")
    public ResponseEntity<?> testVerificationEmail(@RequestParam String email) {
        log.info("📧 Testing verification email to: {}", email);

        String testCode = generateTestCode();

        try {
            emailService.sendVerificationEmail(email, testCode);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "인증 이메일이 전송되었습니다",
                "email", email,
                "verificationCode", testCode,
                "note", "실제 이메일함을 확인하세요 (Sandbox 모드에서는 인증된 이메일만 수신 가능)"
            ));
        } catch (Exception e) {
            log.error("Failed to send verification email", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "이메일 전송 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 환영 이메일 테스트
     *
     * @param email 수신자 이메일
     * @param name 사용자 이름
     * @return 성공 메시지
     */
    @PostMapping("/welcome")
    public ResponseEntity<?> testWelcomeEmail(
            @RequestParam String email,
            @RequestParam(defaultValue = "테스터") String name) {

        log.info("🎉 Testing welcome email to: {} (name: {})", email, name);

        try {
            emailService.sendWelcomeEmail(email, name);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "환영 이메일이 전송되었습니다",
                "email", email,
                "name", name,
                "note", "실제 이메일함을 확인하세요 (Sandbox 모드에서는 인증된 이메일만 수신 가능)"
            ));
        } catch (Exception e) {
            log.error("Failed to send welcome email", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "이메일 전송 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 현재 활성화된 이메일 서비스 정보
     */
    @GetMapping("/info")
    public ResponseEntity<?> getEmailServiceInfo() {
        String serviceType = emailService.getClass().getSimpleName();

        return ResponseEntity.ok(Map.of(
            "activeEmailService", serviceType,
            "description", getServiceDescription(serviceType),
            "endpoints", Map.of(
                "verification", "POST /api/test/email/verification?email={email}",
                "welcome", "POST /api/test/email/welcome?email={email}&name={name}"
            )
        ));
    }

    private String getServiceDescription(String serviceType) {
        return switch (serviceType) {
            case "SesEmailService" -> "AWS SES를 사용하여 실제 이메일을 전송합니다";
            case "SmtpEmailService" -> "SMTP(Gmail 등)를 사용하여 실제 이메일을 전송합니다";
            case "LocalEmailService" -> "콘솔에만 출력하고 실제 이메일을 전송하지 않습니다";
            default -> "알 수 없는 이메일 서비스";
        };
    }

    private String generateTestCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
}
