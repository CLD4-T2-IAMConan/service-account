package com.company.account.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * AWS SES 클라이언트 설정
 */
@Slf4j
@Configuration
@Profile("ses")
public class AwsSesConfig {

    @Value("${aws.ses.region:us-east-1}")
    private String region;

    @Value("${aws.ses.access-key:}")
    private String accessKey;

    @Value("${aws.ses.secret-key:}")
    private String secretKey;

    @Bean
    public SesClient sesClient() {
        log.info("🔧 Initializing AWS SES Client for region: {}", region);

        var clientBuilder = SesClient.builder()
                .region(Region.of(region));

        // 명시적인 자격 증명이 제공된 경우 사용
        if (accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
            log.info("📌 Using explicit AWS credentials");
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            clientBuilder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        } else {
            log.info("📌 Using default AWS credentials chain (IAM Role, Environment, etc.)");
        }

        SesClient client = clientBuilder.build();
        log.info("✅ AWS SES Client initialized successfully");
        return client;
    }
}
