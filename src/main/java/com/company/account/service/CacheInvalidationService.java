package com.company.account.service;

import com.company.account.util.CacheKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Cache invalidation service
 * Manages cache invalidation when user data changes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheInvalidationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;

    /**
     * Invalidate all user-related caches
     * Called when user data is updated or deleted
     */
    public void invalidateUserCaches(Long userId) {
        log.info("Invalidating all caches for user: {}", userId);

        // Invalidate user info cache
        String userKey = cacheKeyGenerator.userKey(userId);
        redisTemplate.delete(userKey);

        // Invalidate refresh token cache
        String refreshTokenKey = cacheKeyGenerator.refreshTokenKey(userId);
        redisTemplate.delete(refreshTokenKey);

        log.debug("Invalidated caches - user: {}, refreshToken: {}", userKey, refreshTokenKey);
    }

    /**
     * Invalidate token validation cache
     * Called when token needs to be invalidated
     */
    public void invalidateTokenValidationCache(String token) {
        log.info("Invalidating token validation cache");

        String tokenKey = cacheKeyGenerator.tokenValidationKey(token);
        redisTemplate.delete(tokenKey);

        log.debug("Invalidated token validation cache: {}", tokenKey);
    }

    /**
     * Invalidate refresh token cache only
     * Called during logout or token refresh
     */
    public void invalidateRefreshTokenCache(Long userId) {
        log.info("Invalidating refresh token cache for user: {}", userId);

        String refreshTokenKey = cacheKeyGenerator.refreshTokenKey(userId);
        redisTemplate.delete(refreshTokenKey);

        log.debug("Invalidated refresh token cache: {}", refreshTokenKey);
    }

    /**
     * Invalidate user info cache only
     * Called when user profile is updated
     */
    public void invalidateUserInfoCache(Long userId) {
        log.info("Invalidating user info cache for user: {}", userId);

        String userKey = cacheKeyGenerator.userKey(userId);
        redisTemplate.delete(userKey);

        log.debug("Invalidated user info cache: {}", userKey);
    }
}
