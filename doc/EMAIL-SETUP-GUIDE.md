# 이메일 전송 설정 가이드

## 📧 이메일 전송 방식

Passit Service Account는 두 가지 이메일 전송 방식을 지원합니다:

1. **콘솔 로그 방식 (기본)** - 개발/테스트용
2. **SMTP 실제 전송** - 프로덕션/실제 테스트용

---

## 🔧 1. 콘솔 로그 방식 (기본)

### 설정
- **프로필:** `default`, `local`, `dev`
- **구현체:** `LocalEmailService`
- **특징:** 실제 이메일을 전송하지 않고 서버 콘솔에 출력

### 실행 방법
```bash
./gradlew bootRun
```

또는

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 콘솔 출력 예시
```
===================================================
📧 [이메일 인증 코드 전송]
수신자: test@example.com
인증 코드: 123456
유효 시간: 10분
===================================================
```

---

## 📨 2. SMTP 실제 전송 (Gmail)

### 사전 준비

#### Step 1: Google 계정 2단계 인증 활성화
1. Google 계정 설정: https://myaccount.google.com
2. 보안 → 2단계 인증 활성화

#### Step 2: 앱 비밀번호 생성
1. Google 계정 설정: https://myaccount.google.com/apppasswords
2. 앱 선택: "메일"
3. 기기 선택: "기타 (맞춤 이름)"
4. 이름 입력: "Passit Service"
5. **16자리 비밀번호 복사** (공백 제거)

### 환경 변수 설정

#### Option 1: .env 파일 사용
`.env` 파일을 생성하고 다음 내용을 추가:

```bash
# Email Configuration
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=abcdefghijklmnop  # 16자리 앱 비밀번호 (공백 제거)
```

#### Option 2: 시스템 환경 변수
```bash
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=abcdefghijklmnop
```

#### Option 3: 실행 시 직접 전달
```bash
./gradlew bootRun --args='--spring.profiles.active=smtp --spring.mail.username=your-email@gmail.com --spring.mail.password=abcdefghijklmnop'
```

### 실행 방법

**SMTP 프로필로 실행:**
```bash
./gradlew bootRun --args='--spring.profiles.active=smtp'
```

**환경 변수와 함께 실행:**
```bash
MAIL_USERNAME=your-email@gmail.com \
MAIL_PASSWORD=abcdefghijklmnop \
./gradlew bootRun --args='--spring.profiles.active=smtp'
```

### 전송 확인
로그에서 다음과 같은 메시지 확인:
```
📧 Sending verification email to: test@example.com
✅ Verification email sent successfully to: test@example.com
```

---

## 🎨 HTML 이메일 템플릿

### 인증 코드 이메일
- **제목:** [Passit] 이메일 인증 코드
- **내용:**
  - 브랜드 로고
  - 6자리 인증 코드 (큰 글씨)
  - 유효 시간 안내 (10분)
  - 보안 경고 메시지

### 환영 이메일
- **제목:** [Passit] 가입을 환영합니다!
- **내용:**
  - 환영 메시지
  - 서비스 주요 기능 소개
  - CTA 버튼

---

## 🔍 문제 해결

### 1. "Username and Password not accepted" 오류
**원인:** 일반 Gmail 비밀번호를 사용
**해결:** 앱 비밀번호를 생성하여 사용

### 2. "Could not authenticate" 오류
**원인:** 2단계 인증 미활성화
**해결:** Google 계정에서 2단계 인증 활성화

### 3. "Connection timeout" 오류
**원인:** 방화벽 또는 네트워크 문제
**해결:**
- 포트 587 열려있는지 확인
- 프록시 설정 확인
- 네트워크 연결 확인

### 4. 이메일이 스팸함으로 이동
**원인:** 발신자 신뢰도 부족
**해결:**
- 수신자의 스팸함 확인
- Gmail에서 "스팸 아님" 처리
- SPF, DKIM 레코드 설정 (프로덕션)

---

## 🚀 프로덕션 배포 시 권장 사항

### 1. AWS SES 사용
Gmail은 개발/테스트용으로만 사용하고, 프로덕션에서는 AWS SES 사용 권장

**장점:**
- 더 높은 전송 한도
- 더 나은 전송률
- 상세한 모니터링
- 비용 효율적

### 2. 환경 변수 관리
민감한 정보는 반드시 환경 변수로 관리:
- AWS Systems Manager Parameter Store
- AWS Secrets Manager
- Kubernetes Secrets

### 3. 이메일 템플릿 개선
- 외부 템플릿 엔진 사용 (Thymeleaf, FreeMarker)
- 다국어 지원
- 개인화된 내용

### 4. 모니터링
- 전송 실패 로깅
- 재시도 메커니즘
- 알림 설정

---

## 📝 테스트 방법

### 1. 콘솔 로그 테스트
```bash
# 서버 실행
./gradlew bootRun

# 회원가입 API 호출
curl -X POST http://localhost:8081/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "name": "홍길동"
  }'

# 콘솔에서 인증 코드 확인
```

### 2. SMTP 실제 전송 테스트
```bash
# SMTP 프로필로 실행
MAIL_USERNAME=your-email@gmail.com \
MAIL_PASSWORD=your-app-password \
./gradlew bootRun --args='--spring.profiles.active=smtp'

# 회원가입 API 호출
curl -X POST http://localhost:8081/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "real-email@example.com",
    "password": "password123",
    "name": "홍길동"
  }'

# 이메일 수신함 확인
```

---

## 🔒 보안 주의사항

1. **.env 파일을 Git에 커밋하지 마세요**
   ```bash
   # .gitignore에 추가
   .env
   ```

2. **앱 비밀번호는 절대 코드에 하드코딩하지 마세요**

3. **정기적으로 앱 비밀번호를 교체하세요**

4. **사용하지 않는 앱 비밀번호는 삭제하세요**

---

## 📚 참고 자료

- [Google 앱 비밀번호 만들기](https://support.google.com/accounts/answer/185833)
- [Gmail SMTP 설정](https://support.google.com/mail/answer/7126229)
- [Spring Boot Mail 문서](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email)
- [AWS SES 시작하기](https://docs.aws.amazon.com/ses/latest/dg/getting-started.html)

---

## 💡 FAQ

**Q: 개발 환경에서는 어느 방식을 사용해야 하나요?**
A: 콘솔 로그 방식 (기본)을 사용하세요. 빠르고 설정이 필요 없습니다.

**Q: 실제 이메일 전송을 테스트하려면?**
A: SMTP 프로필을 사용하되, Gmail 앱 비밀번호를 설정해야 합니다.

**Q: 프로덕션에서는 어떻게 해야 하나요?**
A: AWS SES를 사용하고, 별도의 `AwsSesEmailService` 구현체를 추가하세요.

**Q: 이메일이 발송되지 않아요**
A:
1. 로그 확인
2. 앱 비밀번호 재확인
3. 2단계 인증 활성화 확인
4. 네트워크 연결 확인

---

## 🎯 빠른 시작 체크리스트

### 콘솔 로그 방식 (1분)
- [ ] 서버 실행: `./gradlew bootRun`
- [ ] API 호출
- [ ] 콘솔에서 인증 코드 확인

### SMTP 실제 전송 (5분)
- [ ] Google 2단계 인증 활성화
- [ ] 앱 비밀번호 생성
- [ ] .env 파일 작성
- [ ] SMTP 프로필로 서버 실행
- [ ] API 호출
- [ ] 이메일 수신 확인

완료! 🎉
