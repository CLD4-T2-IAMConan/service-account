package com.company.serviceaccount.cache;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Testcontainers를 사용하여 Valkey 컨테이너를 자동으로 시작하는 테스트 베이스 클래스
 * 
 * Docker가 필요하지만, 별도의 설정 없이 테스트를 실행할 수 있습니다.
 */
@Testcontainers
public abstract class LocalRedisTest extends AbstractRedisTest {

    @Container
    static final GenericContainer<?> valkey = new GenericContainer<>(
        DockerImageName.parse("valkey/valkey:7.2-alpine")
    )
        .withExposedPorts(6379)
        .withReuse(true)
        .withStartupTimeout(Duration.ofMinutes(2));

    @BeforeAll
    static void ensureContainerReady() {
        // 컨테이너가 시작될 때까지 대기
        if (!valkey.isRunning()) {
            valkey.start();
        }
        
        // 컨테이너가 완전히 준비될 때까지 대기
        try {
            Thread.sleep(2000); // 2초 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Testcontainers의 동적 포트 사용
        // @BeforeAll에서 컨테이너가 시작되므로 안전하게 접근 가능
        registry.add("spring.data.redis.host", valkey::getHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(valkey.getMappedPort(6379)));
        registry.add("spring.data.redis.password", () -> "");
    }
}

