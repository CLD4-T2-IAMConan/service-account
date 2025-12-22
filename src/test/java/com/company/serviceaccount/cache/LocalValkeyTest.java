package com.company.serviceaccount.cache;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 로컬에서 실행 중인 Valkey/Redis에 연결하는 테스트 베이스 클래스
 * 
 * 사용 방법:
 * 1. 로컬에서 Valkey 빌드 및 실행:
 *    git clone https://github.com/valkey-io/valkey.git
 *    cd valkey
 *    make
 *    ./src/valkey-server
 * 
 * 2. 또는 Docker Compose로 로컬 Valkey 실행:
 *    docker-compose up -d valkey
 * 
 * 3. 환경 변수 설정:
 *    export REDIS_HOST=localhost
 *    export REDIS_PORT=6380  # docker-compose의 경우
 *    export REDIS_PORT=6379  # 직접 실행한 경우
 * 
 * 4. 테스트 실행:
 *    ./gradlew integrationTest --tests "*Cache*"
 */
public abstract class LocalValkeyTest extends AbstractRedisTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // 환경 변수에서 로컬 Valkey 설정 가져오기
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        String redisPort = System.getenv().getOrDefault("REDIS_PORT", "6379");
        String redisPassword = System.getenv().getOrDefault("REDIS_PASSWORD", "");
        
        registry.add("spring.data.redis.host", () -> redisHost);
        registry.add("spring.data.redis.port", () -> redisPort);
        registry.add("spring.data.redis.password", () -> redisPassword);
        
        System.out.println("Using local Valkey/Redis at " + redisHost + ":" + redisPort);
    }
}

