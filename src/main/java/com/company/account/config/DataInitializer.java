package com.company.account.config;

import com.company.account.entity.User;
import com.company.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 초기 데이터를 생성하는 컴포넌트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.name}")
    private String adminName;

    @Value("${app.admin.nickname}")
    private String adminNickname;

    @Override
    public void run(String... args) {
        createDefaultAdminUser();
    }

    /**
     * 기본 관리자 계정 생성
     * 환경변수를 통해 설정 가능:
     * - ADMIN_EMAIL: 관리자 이메일
     * - ADMIN_PASSWORD: 관리자 비밀번호
     * - ADMIN_NAME: 관리자 이름
     * - ADMIN_NICKNAME: 관리자 닉네임
     */
    private void createDefaultAdminUser() {
        // 이미 관리자 계정이 존재하는지 확인
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Default admin user already exists: {}", adminEmail);
            return;
        }

        // 관리자 계정 생성
        User admin = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .name(adminName)
                .nickname(adminNickname)
                .role(User.UserRole.ADMIN)
                .status(User.UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

        userRepository.save(admin);
        log.info("============================================");
        log.info("Default admin user created successfully!");
        log.info("Email: {}", adminEmail);
        log.info("Name: {}", adminName);
        log.info("============================================");
    }
}
