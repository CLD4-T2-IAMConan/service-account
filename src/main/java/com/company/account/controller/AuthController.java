package com.company.account.controller;

import com.company.account.dto.ApiResponse;
import com.company.account.dto.AuthRequest;
import com.company.account.dto.AuthResponse;
import com.company.account.dto.UserResponse;
import com.company.account.entity.User;
import com.company.account.service.AuthService;
import com.company.account.service.EmailVerificationService;
import com.company.account.service.KakaoAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final KakaoAuthService kakaoAuthService;

    @Value("${kakao.frontend-url:http://localhost:3000}")
    private String frontendUrl;

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

    /**
     * 카카오 로그인 시작 (카카오 로그인 페이지로 리다이렉트)
     * GET /api/auth/kakao
     */
    @GetMapping("/kakao")
    public ResponseEntity<Void> kakaoLogin() {
        log.info("Request to start Kakao login");
        String kakaoLoginUrl = kakaoAuthService.getKakaoLoginUrl();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", kakaoLoginUrl)
                .build();
    }

    /**
     * 카카오 로그인 콜백
     * GET /api/auth/kakao/callback
     */
    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> kakaoCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription) {
        log.info("Kakao callback received. Code: {}, Error: {}", code, error);

        if (error != null) {
            log.error("Kakao login error: {}", errorDescription);
            // 에러 발생 시 프론트엔드 로그인 페이지로 리다이렉트
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", getFrontendUrl() + "/auth?error=kakao_login_failed")
                    .build();
        }

        if (code == null || code.isEmpty()) {
            log.error("Kakao authorization code is missing");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", getFrontendUrl() + "/auth?error=kakao_login_failed")
                    .build();
        }

        try {
            // 카카오 로그인 처리
            AuthResponse.LoginResponse loginResponse = authService.loginWithKakao(code);

            // 프론트엔드로 리다이렉트 (토큰을 쿼리 파라미터로 전달)
            String redirectUrl = String.format(
                    "%s/auth/kakao/callback?token=%s&refreshToken=%s&userId=%d&email=%s&name=%s",
                    getFrontendUrl(),
                    loginResponse.getAccessToken(),
                    loginResponse.getRefreshToken(),
                    loginResponse.getUserId(),
                    loginResponse.getEmail(),
                    loginResponse.getName()
            );

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", redirectUrl)
                    .build();
        } catch (Exception e) {
            log.error("Error processing Kakao login", e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", getFrontendUrl() + "/auth?error=kakao_login_failed&message=" + e.getMessage())
                    .build();
        }
    }

    /**
     * 프론트엔드 URL 가져오기
     */
    private String getFrontendUrl() {
        return frontendUrl;
    }
}
