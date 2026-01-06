# SNS/SQS 테스트 API 사용 가이드

## 문제 해결

"No static resource" 오류가 발생하는 경우:

### 1. 애플리케이션 재시작 필요

컨트롤러가 추가되었으므로 애플리케이션을 재시작해야 합니다:

```bash
cd service-account

# Gradle로 빌드
./gradlew build

# 애플리케이션 재시작
./gradlew bootRun
```

### 2. 프로파일 확인

컨트롤러는 프로파일 제한 없이 항상 활성화됩니다. 하지만 AWS SDK 클라이언트가 필요하므로 빌드가 완료되어야 합니다.

### 3. 빌드 확인

AWS SDK 의존성이 추가되었는지 확인:

```bash
./gradlew dependencies | grep -i "sns\|sqs"
```

## API 엔드포인트

### 1. SNS Topic 목록 조회
```bash
curl http://localhost:8081/api/test/sns-sqs/topics
```

### 2. SQS Queue 목록 조회
```bash
curl http://localhost:8081/api/test/sns-sqs/queues
```

### 3. 이벤트 발행
```bash
curl -X POST "http://localhost:8081/api/test/sns-sqs/publish?topicName=deal-events&eventType=deal.requested" \
  -H "Content-Type: application/json" \
  -d '{"dealId": 123, "buyerId": 1, "sellerId": 2}'
```

### 4. Queue 메시지 확인
```bash
curl "http://localhost:8081/api/test/sns-sqs/queues/chat-deal-events/messages"
```

## 배포된 환경에서 테스트

```bash
# ALB를 통한 접근
ALB_URL="http://passit-dev-alb-1334335211.ap-northeast-2.elb.amazonaws.com"

curl ${ALB_URL}/api/test/sns-sqs/topics
curl ${ALB_URL}/api/test/sns-sqs/queues
```

