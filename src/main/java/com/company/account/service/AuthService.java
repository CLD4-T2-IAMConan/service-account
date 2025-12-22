package com.company.account.service;

import com.company.account.dto.AuthRequest;
import com.company.account.dto.AuthResponse;
import com.company.account.dto.KakaoUserInfo;
import com.company.account.dto.RefreshTokenCache;
import com.company.account.entity.User;
import com.company.account.repository.UserRepository;
import com.company.account.security.JwtTokenProvider;
import com.company.account.util.CacheKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationService emailVerificationService;
    private final KakaoAuthService kakaoAuthService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;
    private final CacheInvalidationService cacheInvalidationService;

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

        // Refresh Token 저장 (DB)
        user.setRefreshToken(refreshToken);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Refresh Token 메타정보 캐싱 (Redis)
        String refreshTokenKey = cacheKeyGenerator.refreshTokenKey(user.getUserId());
        RefreshTokenCache tokenCache = RefreshTokenCache.builder()
            .userId(user.getUserId())
            .token(refreshToken)
            .isValid(true)
            .expiredAt(LocalDateTime.now().plusDays(7))
            .build();
        redisTemplate.opsForValue().set(
            refreshTokenKey,
            tokenCache,
            7,
            TimeUnit.DAYS
        );

        log.info("User logged in successfully: {}", user.getUserId());

        return AuthResponse.LoginResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .provider(user.getProvider() != null ? user.getProvider().name() : null)
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

        // Refresh Token 제거 (DB)
        user.setRefreshToken(null);
        userRepository.save(user);

        // Refresh Token 캐시 무효화 (Redis)
        cacheInvalidationService.invalidateRefreshTokenCache(userId);

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

        // 1. Redis 캐시 확인
        String refreshTokenKey = cacheKeyGenerator.refreshTokenKey(userId);
        RefreshTokenCache cachedToken = (RefreshTokenCache) redisTemplate.opsForValue().get(refreshTokenKey);

        if (cachedToken != null && cachedToken.getToken().equals(refreshToken) && cachedToken.getIsValid()) {
            log.debug("Refresh token found in cache for user: {}", userId);

            // 캐시에서 가져온 경우, DB 조회는 사용자 정보만 (Refresh Token 비교 스킵)
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

            // 새로운 Access Token 생성
            String newAccessToken = jwtTokenProvider.createAccessToken(
                    user.getUserId(),
                    user.getEmail(),
                    user.getRole().name()
            );

            log.info("Access token refreshed successfully for user: {}", userId);

            return AuthResponse.TokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();
        }

        // 2. 캐시 미스 시 DB 조회 및 검증
        log.debug("Refresh token not found in cache, querying database for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다");
        }

        // 캐시 재생성
        RefreshTokenCache newCache = RefreshTokenCache.builder()
            .userId(user.getUserId())
            .token(refreshToken)
            .isValid(true)
            .expiredAt(LocalDateTime.now().plusDays(7))
            .build();
        redisTemplate.opsForValue().set(refreshTokenKey, newCache, 7, TimeUnit.DAYS);

        // 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name()
        );

        log.info("Access token refreshed successfully for user: {}", userId);

        return AuthResponse.TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    /**
     * 카카오 로그인
     * 같은 이메일로 이미 가입한 계정이 있으면 기존 계정에 카카오 연동
     */
    @Transactional
    public AuthResponse.LoginResponse loginWithKakao(String code) {
        log.info("Kakao login attempt with code: {}", code);

        // 1. 인증 코드로 액세스 토큰 받기
        String accessToken = kakaoAuthService.getAccessToken(code);

        // 2. 액세스 토큰으로 사용자 정보 가져오기
        KakaoUserInfo kakaoUserInfo = kakaoAuthService.getUserInfo(accessToken);

        if (kakaoUserInfo.getKakaoAccount() == null || 
            kakaoUserInfo.getKakaoAccount().getEmail() == null) {
            throw new IllegalArgumentException("카카오 이메일 정보를 가져올 수 없습니다");
        }

        String email = kakaoUserInfo.getKakaoAccount().getEmail();
        String nickname = kakaoUserInfo.getKakaoAccount().getProfile() != null 
            ? kakaoUserInfo.getKakaoAccount().getProfile().getNickname() 
            : null;
        String profileImageUrl = kakaoUserInfo.getKakaoAccount().getProfile() != null
            ? kakaoUserInfo.getKakaoAccount().getProfile().getProfileImageUrl()
            : null;

        log.info("Kakao user info - Email: {}, Nickname: {}", email, nickname);

        // 3. 기존 사용자 조회
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            // 기존 계정이 있는 경우
            if (user.getStatus() == User.UserStatus.DELETED) {
                // 탈퇴한 계정인 경우: 재가입 처리 (계정 재활성화)
                log.info("Deleted user found. Reactivating account for user ID: {}", user.getUserId());
                
                // 계정 재활성화
                user.setStatus(User.UserStatus.ACTIVE);
                user.setDeletedAt(null);
                
                // 카카오 정보로 업데이트
                user.setProvider(User.SocialProvider.KAKAO);
                user.setName(nickname != null ? nickname : user.getName());
                
                // 닉네임 중복 체크 및 업데이트
                String finalNickname = nickname;
                if (finalNickname == null || finalNickname.trim().isEmpty()) {
                    finalNickname = "카카오_" + kakaoUserInfo.getId();
                }
                
                // 기존 닉네임과 다르고 중복되는 경우 숫자 추가
                if (!finalNickname.equals(user.getNickname()) && userRepository.existsByNickname(finalNickname)) {
                    String baseNickname = finalNickname;
                    int suffix = 1;
                    while (userRepository.existsByNickname(finalNickname)) {
                        finalNickname = baseNickname + "_" + suffix;
                        suffix++;
                    }
                }
                user.setNickname(finalNickname);
                
                if (profileImageUrl != null) {
                    user.setProfileImageUrl(profileImageUrl);
                }
                
                // 비밀번호 재설정 (카카오 로그인용)
                user.setPassword(passwordEncoder.encode("KAKAO_" + kakaoUserInfo.getId() + "_" + System.currentTimeMillis()));
                
                // 이메일 인증 상태 업데이트
                user.setEmailVerified(true);
                user.setEmailVerifiedAt(LocalDateTime.now());
                
                user = userRepository.save(user);
                log.info("Deleted user reactivated with ID: {}", user.getUserId());
            } else if (user.getStatus() == User.UserStatus.SUSPENDED) {
                // 정지된 계정은 재가입 불가
                throw new IllegalArgumentException("정지된 계정입니다");
            } else {
                // ACTIVE 상태인 경우: 카카오 연동
                log.info("Existing active user found. Linking Kakao account to user ID: {}", user.getUserId());

                // 카카오 연동 정보 업데이트
                user.setProvider(User.SocialProvider.KAKAO);
                if (profileImageUrl != null && user.getProfileImageUrl() == null) {
                    user.setProfileImageUrl(profileImageUrl);
                }
                // 이메일 인증 상태 업데이트 (카카오는 이메일 인증 완료로 간주)
                if (!user.getEmailVerified()) {
                    user.setEmailVerified(true);
                    user.setEmailVerifiedAt(LocalDateTime.now());
                }
            }
        } else {
            // 신규 사용자인 경우: 회원가입
            log.info("New user. Creating account with Kakao");

            // 닉네임 중복 체크 및 생성
            String finalNickname = nickname;
            if (finalNickname == null || finalNickname.trim().isEmpty()) {
                finalNickname = "카카오_" + kakaoUserInfo.getId();
            }
            
            // 닉네임 중복 시 숫자 추가
            String baseNickname = finalNickname;
            int suffix = 1;
            while (userRepository.existsByNickname(finalNickname)) {
                finalNickname = baseNickname + "_" + suffix;
                suffix++;
            }

            // 사용자 생성 (카카오 로그인은 비밀번호 없음)
            user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode("KAKAO_" + kakaoUserInfo.getId() + "_" + System.currentTimeMillis())) // 임시 비밀번호
                    .name(nickname != null ? nickname : "카카오 사용자")
                    .nickname(finalNickname)
                    .provider(User.SocialProvider.KAKAO)
                    .profileImageUrl(profileImageUrl)
                    .emailVerified(true) // 카카오는 이메일 인증 완료로 간주
                    .emailVerifiedAt(LocalDateTime.now())
                    .build();

            user = userRepository.save(user);
            log.info("New user created with ID: {}", user.getUserId());
        }

        // 4. JWT 토큰 생성
        String jwtAccessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name()
        );
        String jwtRefreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // Refresh Token 저장 (DB)
        user.setRefreshToken(jwtRefreshToken);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Refresh Token 메타정보 캐싱 (Redis)
        String refreshTokenKey = cacheKeyGenerator.refreshTokenKey(user.getUserId());
        RefreshTokenCache tokenCache = RefreshTokenCache.builder()
            .userId(user.getUserId())
            .token(jwtRefreshToken)
            .isValid(true)
            .expiredAt(LocalDateTime.now().plusDays(7))
            .build();
        redisTemplate.opsForValue().set(
            refreshTokenKey,
            tokenCache,
            7,
            TimeUnit.DAYS
        );

        log.info("Kakao login successful. User ID: {}", user.getUserId());

        return AuthResponse.LoginResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .provider(user.getProvider() != null ? user.getProvider().name() : null)
                .accessToken(jwtAccessToken)
                .refreshToken(jwtRefreshToken)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }
}
