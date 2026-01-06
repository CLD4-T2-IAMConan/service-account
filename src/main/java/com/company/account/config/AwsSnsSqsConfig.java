package com.company.account.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * AWS SNS/SQS 클라이언트 설정
 */
@Slf4j
@Configuration
@Profile({"dev", "test", "production"})  // 모든 환경에서 사용 가능하도록 설정
public class AwsSnsSqsConfig {

    @Value("${aws.region:ap-northeast-2}")
    private String region;

    @Value("${aws.sns.access-key:}")
    private String accessKey;

    @Value("${aws.sns.secret-key:}")
    private String secretKey;

    @Bean
    public SnsClient snsClient() {
        log.info("🔧 Initializing AWS SNS Client for region: {}", region);

        SnsClient.Builder builder = SnsClient.builder()
                .region(Region.of(region));

        if (accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
            log.info("📌 Using explicit AWS credentials for SNS");
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        } else {
            log.info("📌 Using default AWS credentials chain for SNS (IAM Role, Environment, etc.)");
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        log.info("✅ AWS SNS Client initialized successfully");
        return builder.build();
    }

    @Bean
    public SqsClient sqsClient() {
        log.info("🔧 Initializing AWS SQS Client for region: {}", region);

        SqsClient.Builder builder = SqsClient.builder()
                .region(Region.of(region));

        if (accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
            log.info("📌 Using explicit AWS credentials for SQS");
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        } else {
            log.info("📌 Using default AWS credentials chain for SQS (IAM Role, Environment, etc.)");
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        log.info("✅ AWS SQS Client initialized successfully");
        return builder.build();
    }
}

