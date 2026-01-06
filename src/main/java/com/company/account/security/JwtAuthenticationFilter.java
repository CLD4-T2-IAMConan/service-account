package com.company.account.security;

import com.company.account.dto.TokenValidationCache;
import com.company.account.util.CacheKeyGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/email/") ||
               path.startsWith("/api/test/") ||
               path.equals("/api/auth/health") ||
               path.startsWith("/actuator/") ||
               path.equals("/error");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                // 1. 토큰 해시 생성
                String tokenHash = cacheKeyGenerator.hashToken(jwt);
                String cacheKey = cacheKeyGenerator.tokenValidationKey(tokenHash);

                // 2. 캐시에서 검증 결과 조회
                TokenValidationCache cachedValidation =
                    (TokenValidationCache) redisTemplate.opsForValue().get(cacheKey);

                Long userId;
                String email;
                String role;

                if (cachedValidation != null) {
                    // 캐시 히트: 검증 결과 사용
                    log.debug("Token validation cache hit for hash: {}", tokenHash.substring(0, 8));
                    userId = cachedValidation.getUserId();
                    email = cachedValidation.getEmail();
                    role = cachedValidation.getRole();
                } else {
                    // 캐시 미스: 토큰 검증 수행
                    log.debug("Token validation cache miss for hash: {}", tokenHash.substring(0, 8));

                    if (!jwtTokenProvider.validateToken(jwt)) {
                        filterChain.doFilter(request, response);
                        return;
                    }

                    userId = jwtTokenProvider.getUserIdFromToken(jwt);
                    email = jwtTokenProvider.getEmailFromToken(jwt);
                    role = jwtTokenProvider.getRoleFromToken(jwt);

                    // 검증 결과 캐싱 (5분)
                    TokenValidationCache validationCache = TokenValidationCache.builder()
                        .userId(userId)
                        .email(email)
                        .role(role)
                        .expiredAt(LocalDateTime.now().plusMinutes(5))
                        .build();
                    redisTemplate.opsForValue().set(cacheKey, validationCache, 5, TimeUnit.MINUTES);
                }

                // Spring Security의 Authority 형식으로 변환 (ROLE_ 접두사 추가)
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.singletonList(authority)
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Set Authentication for user: {} with role: {}", email, role);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Request Header에서 JWT 토큰 추출
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
