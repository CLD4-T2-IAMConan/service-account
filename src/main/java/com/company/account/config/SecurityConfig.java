package com.company.account.config;

import com.company.account.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private String allowedOriginsConfig;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF 비활성화 (JWT 사용)
                .csrf(AbstractHttpConfigurer::disable)

                // 세션 사용 안 함 (Stateless)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 인증 불필요 엔드포인트
                        .requestMatchers(
                                "/api/auth/**",          // 인증 관련
                                "/api/email/**",         // 이메일 인증
                                "/api/test/**",          // 테스트 엔드포인트 (dev, ses, smtp 프로파일에서만 활성화됨)
                                "/api/auth/health",      // Health Check (Path 기반 라우팅)
                                "/actuator/**",          // Actuator Health Check
                                "/error"
                        ).permitAll()
                        // Prometheus 메트릭 엔드포인트 허용
                        .requestMatchers("/actuator/prometheus").permitAll()
                        // 사용자 조회 (GET) - 채팅방에서 판매자 정보 조회용
                        .requestMatchers("GET", "/api/users/{userId}").permitAll()
                        .requestMatchers("GET", "/api/users/email/**").permitAll()
                        // 내 정보 관리 엔드포인트 (인증된 사용자만)
                        .requestMatchers("/api/users/me/**").authenticated()
                        // 관리자 전용 엔드포인트 (회원 관리)
                        // TODO: 프로덕션 환경에서는 hasRole("ADMIN")으로 변경 필요
                        .requestMatchers("/api/users/search").authenticated()  // 임시: 테스트용
                        .requestMatchers("/api/users/**").authenticated()  // 나머지 사용자 API는 인증 필요
                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                // JWT 인증 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // application.yml 또는 환경 변수에서 허용할 Origin 목록 가져오기
        // 환경 변수 CORS_ALLOWED_ORIGINS가 있으면 우선 사용, 없으면 application.yml의 cors.allowed-origins 사용
        String allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = allowedOriginsConfig;
        }
        
        // 쉼표로 구분된 Origin 목록을 리스트로 변환 (공백 제거)
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        // CORS preflight 요청에 대한 응답 헤더 노출
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
