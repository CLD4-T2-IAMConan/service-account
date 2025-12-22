package com.company.serviceaccount.cache;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * AWS ElastiCache (Valkey)에 연결하는 테스트 베이스 클래스
 * 
 * ⚠️ 주의: 이 서비스는 EKS로 배포되므로, ElastiCache는 VPC 내부에 있습니다.
 * 로컬에서 직접 연결하려면 VPN 또는 터널링이 필요합니다.
 * 
 * 권장 사용 방법:
 * 1. 로컬 개발 중: LocalRedisTest 사용 (Testcontainers) - 가장 간단
 * 2. EKS 배포 후: EKS Pod에서 직접 테스트 실행
 *    - 스크립트 사용: ./scripts/test-in-eks-pod.sh
 *    - 또는 kubectl exec로 직접 실행
 * 
 * 로컬에서 ElastiCache에 직접 연결하는 경우:
 * 
 * 1. 환경 변수 설정:
 *    export ELASTICACHE_ENDPOINT=your-cluster.xxxxx.cache.amazonaws.com
 *    export ELASTICACHE_PORT=6379  # 기본값
 *    export ELASTICACHE_PASSWORD=  # ElastiCache는 기본적으로 비밀번호 없음
 *    export ELASTICACHE_SSL=false   # 기본값
 * 
 * 2. 네트워크 연결 필요:
 *    - VPN 또는 AWS Direct Connect를 통해 VPC에 연결
 *    - 또는 EC2 인스턴스를 통한 SSH 터널링
 *    - 또는 kubectl port-forward (복잡함)
 * 
 * 3. 테스트 클래스에서 사용:
 *    class MyTest extends AwsElastiCacheTest {
 *        // 테스트 코드
 *    }
 * 
 * 4. 테스트 실행:
 *    ./gradlew test --tests "*Cache*"
 * 
 * 자세한 내용은 doc/AWS-ELASTICACHE-TESTING.md 참조
 */
public abstract class AwsElastiCacheTest extends AbstractRedisTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // 환경 변수에서 ElastiCache 설정 가져오기
        String endpoint = System.getenv("ELASTICACHE_ENDPOINT");
        String port = System.getenv().getOrDefault("ELASTICACHE_PORT", "6379");
        String password = System.getenv().getOrDefault("ELASTICACHE_PASSWORD", "");
        String sslEnabled = System.getenv().getOrDefault("ELASTICACHE_SSL", "false");
        
        // 엔드포인트가 설정되지 않은 경우 경고
        if (endpoint == null || endpoint.isEmpty()) {
            System.err.println("WARNING: ELASTICACHE_ENDPOINT environment variable is not set!");
            System.err.println("Please set ELASTICACHE_ENDPOINT to your ElastiCache primary endpoint.");
            System.err.println("Example: export ELASTICACHE_ENDPOINT=your-cluster.xxxxx.cache.amazonaws.com");
        }
        
        registry.add("spring.data.redis.host", () -> endpoint != null ? endpoint : "localhost");
        registry.add("spring.data.redis.port", () -> port);
        registry.add("spring.data.redis.password", () -> password);
        registry.add("spring.data.redis.ssl.enabled", () -> sslEnabled);
        
        System.out.println("Using AWS ElastiCache at " + endpoint + ":" + port);
        System.out.println("SSL enabled: " + sslEnabled);
    }
}

