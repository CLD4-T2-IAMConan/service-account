package com.company.account.controller;

import com.company.account.dto.ApiResponse;
import com.company.account.dto.AuthRequest;
import com.company.account.dto.AuthResponse;
import com.company.account.dto.UserResponse;
import com.company.account.entity.User;
import com.company.account.service.AuthService;
import com.company.account.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    /**
     * 회원가입
     * POST /api/auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signUp(
            @Valid @RequestBody AuthRequest.SignUp request) {
        log.info("Request to sign up with email: {}", request.getEmail());

        User user = authService.signUp(request);
        UserResponse response = UserResponse.fromEntity(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "회원가입이 완료되었습니다. 이메일 인증을 진행해주세요."));
    }

    /**
     * 이메일 인증 코드 전송
     * POST /api/auth/send-verification-code
     */
    @PostMapping("/send-verification-code")
    public ResponseEntity<ApiResponse<AuthResponse.VerificationCodeSent>> sendVerificationCode(
            @Valid @RequestBody AuthRequest.SendVerificationCode request) {
        log.info("Request to send verification code to: {}", request.getEmail());

        emailVerificationService.sendVerificationCode(request.getEmail());

        AuthResponse.VerificationCodeSent response = AuthResponse.VerificationCodeSent.builder()
                .email(request.getEmail())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "인증 코드가 전송되었습니다"));
    }

    /**
     * 이메일 인증 코드 검증
     * POST /api/auth/verify-email
     */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody AuthRequest.VerifyEmail request) {
        log.info("Request to verify email: {}", request.getEmail());

        emailVerificationService.verifyCode(request.getEmail(), request.getCode());

        return ResponseEntity.ok(ApiResponse.success(null, "이메일 인증이 완료되었습니다"));
    }

    /**
     * 로그인
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse.LoginResponse>> login(
            @Valid @RequestBody AuthRequest.Login request) {
        log.info("Request to login with email: {}", request.getEmail());

        AuthResponse.LoginResponse response = authService.login(request);

        return ResponseEntity.ok(ApiResponse.success(response, "로그인 성공"));
    }

    /**
     * 로그아웃
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader) {
        // TODO: JWT에서 userId 추출하도록 개선 필요
        // 현재는 간단하게 구현
        log.info("Request to logout");

        // 임시로 userId를 1로 설정 (실제로는 JWT에서 추출해야 함)
        // authService.logout(userId);

        return ResponseEntity.ok(ApiResponse.success(null, "로그아웃 성공"));
    }

    /**
     * Access Token 갱신
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse.TokenResponse>> refreshToken(
            @Valid @RequestBody AuthRequest.RefreshToken request) {
        log.info("Request to refresh access token");

        AuthResponse.TokenResponse response = authService.refreshAccessToken(request.getRefreshToken());

        return ResponseEntity.ok(ApiResponse.success(response, "토큰 갱신 성공"));
    }
}
