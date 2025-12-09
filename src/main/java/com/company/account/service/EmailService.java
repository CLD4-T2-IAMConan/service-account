package com.company.account.service;

/**
 * 이메일 전송 서비스 인터페이스
 * 로컬 환경에서는 콘솔 출력, 프로덕션에서는 AWS SES 사용
 */
public interface EmailService {

    /**
     * 이메일 인증 코드 전송
     * @param to 수신자 이메일
     * @param verificationCode 인증 코드
     */
    void sendVerificationEmail(String to, String verificationCode);

    /**
     * 환영 이메일 전송
     * @param to 수신자 이메일
     * @param name 사용자 이름
     */
    void sendWelcomeEmail(String to, String name);
}
