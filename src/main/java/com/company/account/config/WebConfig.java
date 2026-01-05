package com.company.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // CORS 설정은 SecurityConfig.java에서 통합 관리
    // WebMvcConfigurer의 CORS와 Spring Security의 CORS가 충돌하므로
    // Security CORS 설정만 사용 (allowCredentials:true 와 allowedOrigins:* 불가)
}
