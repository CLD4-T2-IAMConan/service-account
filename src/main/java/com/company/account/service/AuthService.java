package com.company.account.service;

import com.company.account.dto.AuthRequest;
import com.company.account.dto.AuthResponse;
import com.company.account.entity.User;
import com.company.account.repository.UserRepository;
import com.company.account.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationService emailVerificationService;

    /**
     * 회원가입 (이메일 인증 필요)
     */
    @Transactional
    public User signUp(AuthRequest.SignUp request) {
        log.info("Signing up user with email: {}", request.getEmail());

        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + request.getEmail());
        }

        // 닉네임 중복 체크
        if (request.getNickname() != null && userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다: " + request.getNickname());
        }

        // 사용자 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .nickname(request.getNickname())
                .emailVerified(true)  // TODO: 임시로 이메일 인증 스킵 (나중에 false로 변경)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User signed up successfully with ID: {}", savedUser.getUserId());

        // TODO: 임시로 이메일 인증 코드 전송 비활성화
        // emailVerificationService.sendVerificationCode(request.getEmail());

        return savedUser;
    }

    /**
     * 로그인
     */
    @Transactional
    public AuthResponse.LoginResponse login(AuthRequest.Login request) {
        log.info("User login attempt: {}", request.getEmail());

        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다"));

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        // TODO: 임시로 이메일 인증 체크 비활성화
        // if (!user.getEmailVerified()) {
        //     throw new IllegalArgumentException("이메일 인증이 필요합니다");
        // }

        // 계정 상태 확인
        if (user.getStatus() == User.UserStatus.DELETED) {
            throw new IllegalArgumentException("삭제된 계정입니다");
        }
        if (user.getStatus() == User.UserStatus.SUSPENDED) {
            throw new IllegalArgumentException("정지된 계정입니다");
        }

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // Refresh Token 저장
        user.setRefreshToken(refreshToken);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User logged in successfully: {}", user.getUserId());

        return AuthResponse.LoginResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusHours(1))  // Access Token 만료 시간
                .build();
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(Long userId) {
        log.info("User logout: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // Refresh Token 제거
        user.setRefreshToken(null);
        userRepository.save(user);

        log.info("User logged out successfully: {}", userId);
    }

    /**
     * Access Token 갱신
     */
    @Transactional
    public AuthResponse.TokenResponse refreshAccessToken(String refreshToken) {
        log.info("Refreshing access token");

        // Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다");
        }

        // 사용자 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // 사용자 조회 및 Refresh Token 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다");
        }

        // 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name()
        );

        log.info("Access token refreshed successfully for user: {}", userId);

        return AuthResponse.TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)  // Refresh Token은 그대로 유지
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }
}
