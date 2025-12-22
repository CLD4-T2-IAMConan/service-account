package com.company.serviceaccount.cache;

/**
 * Redis/Valkey 통합 테스트를 위한 베이스 클래스
 * 
 * 사용 방법:
 * 1. Testcontainers 사용 (기본): LocalRedisTest 사용
 * 2. 로컬 Valkey 사용: LocalValkeyTest 사용
 * 
 * 이 클래스는 추상 클래스로만 사용되며, 실제 설정은 하위 클래스에서 정의됩니다.
 */
public abstract class AbstractRedisTest {
    // 설정은 하위 클래스에서 @DynamicPropertySource로 정의
}
