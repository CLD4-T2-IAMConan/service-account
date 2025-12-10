package com.company.account.service;

import com.company.account.dto.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final RestTemplate restTemplate;

    @Value("${kakao.rest-api-key}")
    private String restApiKey;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Value("${kakao.admin-key}")
    private String adminKey;

    /**
     * 카카오 로그인 URL 생성
     */
    public String getKakaoLoginUrl() {
        String baseUrl = "https://kauth.kakao.com/oauth/authorize";
        
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("client_id", restApiKey)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "profile_nickname,account_email")
                .build()
                .toUriString();
    }

    /**
     * 인증 코드로 액세스 토큰 받기
     */
    public String getAccessToken(String code) {
        log.info("Getting access token from Kakao with code: {}", code);

        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", restApiKey);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        // client_secret이 있으면 추가 (보안 강화)
        if (clientSecret != null && !clientSecret.isEmpty()) {
            params.add("client_secret", clientSecret);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<KakaoUserInfo.TokenResponse> response = restTemplate.postForEntity(
                    tokenUrl, request, KakaoUserInfo.TokenResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Successfully obtained access token");
                return response.getBody().getAccessToken();
            } else {
                log.error("Failed to get access token. Status: {}", response.getStatusCode());
                throw new RuntimeException("카카오 액세스 토큰을 받아오는데 실패했습니다");
            }
        } catch (Exception e) {
            log.error("Error getting access token from Kakao", e);
            throw new RuntimeException("카카오 액세스 토큰을 받아오는데 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 액세스 토큰으로 사용자 정보 가져오기
     */
    public KakaoUserInfo getUserInfo(String accessToken) {
        log.info("Getting user info from Kakao");

        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(
                    userInfoUrl, HttpMethod.GET, request, KakaoUserInfo.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Successfully obtained user info from Kakao. User ID: {}", response.getBody().getId());
                return response.getBody();
            } else {
                log.error("Failed to get user info. Status: {}", response.getStatusCode());
                throw new RuntimeException("카카오 사용자 정보를 가져오는데 실패했습니다");
            }
        } catch (Exception e) {
            log.error("Error getting user info from Kakao", e);
            throw new RuntimeException("카카오 사용자 정보를 가져오는데 실패했습니다: " + e.getMessage());
        }
    }
}
