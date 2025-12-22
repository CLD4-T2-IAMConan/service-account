package com.company.serviceaccount.cache;

import com.company.account.util.CacheKeyGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CacheKeyGenerator 테스트")
class CacheKeyGeneratorTest {

    private final CacheKeyGenerator cacheKeyGenerator = new CacheKeyGenerator();

    @Test
    @DisplayName("토큰 해시 생성 - 동일 토큰은 동일한 해시 반환")
    void hashToken_sameToken_returnsSameHash() {
        // Given
        String token = "test-token-12345";

        // When
        String hash1 = cacheKeyGenerator.hashToken(token);
        String hash2 = cacheKeyGenerator.hashToken(token);

        // Then
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(16); // 축약된 해시 길이
    }

    @Test
    @DisplayName("토큰 해시 생성 - 다른 토큰은 다른 해시 반환")
    void hashToken_differentTokens_returnsDifferentHashes() {
        // Given
        String token1 = "test-token-12345";
        String token2 = "test-token-67890";

        // When
        String hash1 = cacheKeyGenerator.hashToken(token1);
        String hash2 = cacheKeyGenerator.hashToken(token2);

        // Then
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("사용자 ID 기반 캐시 키 생성")
    void userKey_returnsCorrectFormat() {
        // Given
        Long userId = 123L;

        // When
        String key = cacheKeyGenerator.userKey(userId);

        // Then
        assertThat(key).isEqualTo("user:123");
    }

    @Test
    @DisplayName("이메일 기반 캐시 키 생성")
    void userEmailKey_returnsCorrectFormat() {
        // Given
        String email = "test@example.com";

        // When
        String key = cacheKeyGenerator.userEmailKey(email);

        // Then
        assertThat(key).isEqualTo("user:email:test@example.com");
    }

    @Test
    @DisplayName("Refresh Token 캐시 키 생성")
    void refreshTokenKey_returnsCorrectFormat() {
        // Given
        Long userId = 456L;

        // When
        String key = cacheKeyGenerator.refreshTokenKey(userId);

        // Then
        assertThat(key).isEqualTo("auth:refresh:456");
    }

    @Test
    @DisplayName("Token 검증 캐시 키 생성")
    void tokenValidationKey_returnsCorrectFormat() {
        // Given
        String tokenHash = "abc123def456";

        // When
        String key = cacheKeyGenerator.tokenValidationKey(tokenHash);

        // Then
        assertThat(key).isEqualTo("auth:token:abc123def456");
    }

    @Test
    @DisplayName("토큰 해시 생성 - null 안전성")
    void hashToken_nullToken_throwsException() {
        // When & Then
        assertThatThrownBy(() -> cacheKeyGenerator.hashToken(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("토큰 해시 생성 - 빈 문자열")
    void hashToken_emptyString_returnsHash() {
        // Given
        String emptyToken = "";

        // When
        String hash = cacheKeyGenerator.hashToken(emptyToken);

        // Then
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(16);
    }
}

