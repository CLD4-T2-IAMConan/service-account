# Service Account

Passit 프로젝트의 사용자 계정 관리 마이크로서비스입니다.

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [시작하기](#시작하기)
- [API 문서](#api-문서)
- [환경 변수](#환경-변수)
- [개발 규칙](#개발-규칙)

## 프로젝트 개요

Service Account는 Passit 플랫폼의 사용자 인증 및 계정 관리를 담당하는 독립적인 마이크로서비스입니다.

### 주요 책임

- 사용자 회원가입 및 로그인
- JWT 기반 토큰 인증/인가
- 이메일 인증
- 소셜 로그인 (카카오)
- 사용자 프로필 관리
- 계정 상태 관리

## 주요 기능

### 인증 (Authentication)

- **회원가입**: 이메일 기반 회원가입 및 이메일 인증
- **로그인**: 이메일/비밀번호 기반 로그인
- **소셜 로그인**: 카카오 OAuth 2.0 연동
- **토큰 관리**: JWT Access Token 및 Refresh Token 발급/갱신
- **로그아웃**: 토큰 무효화

### 사용자 관리

- **프로필 조회/수정**: 내 정보 조회 및 업데이트
- **비밀번호 관리**: 비밀번호 변경/설정/확인
- **계정 탈퇴**: 소프트 삭제 방식의 계정 탈퇴
- **사용자 검색**: 키워드, 상태별 검색 및 페이지네이션
- **상태 관리**: 활성화/정지/삭제 상태 관리

### 관리자 기능

- 사용자 목록 조회 (검색, 필터링, 페이지네이션)
- 사용자 권한 변경
- 사용자 계정 정지/활성화
- 사용자 영구 삭제

## 기술 스택

### Backend

- **Java**: 17
- **Spring Boot**: 3.2.0
- **Spring Data JPA**: 데이터베이스 접근
- **Spring Security**: 보안 및 인증/인가
- **Spring Validation**: 요청 데이터 검증
- **Spring Mail**: 이메일 발송
- **JWT**: JSON Web Token 인증 (io.jsonwebtoken:jjwt)
- **Lombok**: 보일러플레이트 코드 감소

### Database

- **MySQL**: 8.0+

### Testing

- **JUnit 5**: 단위 테스트
- **Spring Security Test**: 보안 테스트
- **REST Assured**: API 통합 테스트
- **Testcontainers**: 컨테이너 기반 통합 테스트

### Build Tool

- **Gradle**: 8.5

## 시작하기

### 사전 요구사항

다음 도구들이 설치되어 있어야 합니다:

| 도구           | 버전                      | 설치 링크                                                |
| -------------- | ------------------------- | -------------------------------------------------------- |
| IntelliJ IDEA  | 최신 (Community/Ultimate) | [JetBrains](https://www.jetbrains.com/idea/)             |
| JDK            | 17                        | [Adoptium](https://adoptium.net)                         |
| Docker Desktop | 최신                      | [Docker](https://www.docker.com/products/docker-desktop) |
| Git            | 최신                      | [Git](https://git-scm.com)                               |

### 로컬 개발 환경 설정

#### 1. 리포지토리 클론

```bash
git clone https://github.com/your-org/service-account.git
cd service-account
```

#### 2. 데이터베이스 시작 (Docker Compose)

```bash
docker-compose up -d
```

MySQL 컨테이너가 포트 3307에서 실행됩니다.

#### 3. 환경 변수 설정

필요한 경우 `.env` 파일을 생성하거나 환경 변수를 설정하세요:

```bash
# 데이터베이스
export DB_HOST=localhost
export DB_PORT=3307
export DB_NAME=passit_db
export DB_USER=passit_user
export DB_PASSWORD=passit_password

# 이메일 (Gmail)
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password

# 카카오 OAuth
export KAKAO_REST_API_KEY=your-kakao-rest-api-key
export KAKAO_CLIENT_SECRET=your-kakao-client-secret
export KAKAO_REDIRECT_URI=http://localhost:8081/api/auth/kakao/callback
export FRONTEND_URL=http://localhost:3000

# 관리자 계정
export ADMIN_EMAIL=admin@passit.com
export ADMIN_PASSWORD=admin123!
export ADMIN_NAME=관리자
```

#### 4. 애플리케이션 실행

**방법 1: Gradle로 실행**

```bash
./gradlew bootRun
```

**방법 2: IntelliJ IDEA**

1. 프로젝트를 IntelliJ IDEA로 열기
2. `src/main/java/com/company/account/AccountApplication.java` 실행
3. Run Configuration에서 환경 변수 설정 가능

애플리케이션은 `http://localhost:8081`에서 실행됩니다.

#### 5. Health Check 확인

```bash
curl http://localhost:8081/api/health
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "service-account"
  },
  "error": null
}
```

### 빌드 및 테스트

#### 전체 빌드

```bash
./gradlew clean build
```

#### 단위 테스트 실행

```bash
./gradlew test
```

#### 통합 테스트 실행 (Testcontainers)

```bash
./gradlew integrationTest
```

#### 테스트 커버리지 확인

```bash
./gradlew jacocoTestReport
# 리포트 확인: build/reports/jacoco/test/html/index.html
```

## API 문서

### 공통 응답 형식

모든 API는 다음 형식으로 응답합니다:

#### 성공 응답

```json
{
  "success": true,
  "data": {
    /* 실제 데이터 */
  },
  "message": "성공 메시지",
  "error": null
}
```

#### 오류 응답

```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": "에러 메시지"
}
```

### 주요 엔드포인트

#### 인증 API (`/api/auth`)

| Method | Endpoint                       | 설명                        | 인증 필요 |
| ------ | ------------------------------ | --------------------------- | --------- |
| POST   | `/signup`                      | 회원가입                    | ❌        |
| POST   | `/send-verification-code`      | 이메일 인증 코드 전송       | ❌        |
| POST   | `/verify-email`                | 이메일 인증                 | ❌        |
| POST   | `/login`                       | 로그인                      | ❌        |
| POST   | `/logout`                      | 로그아웃                    | ✅        |
| POST   | `/refresh`                     | Access Token 갱신           | ❌        |
| GET    | `/kakao`                       | 카카오 로그인 시작          | ❌        |
| GET    | `/kakao/callback`              | 카카오 로그인 콜백          | ❌        |

#### 사용자 API (`/api/users`)

| Method | Endpoint                  | 설명                       | 인증 필요 |
| ------ | ------------------------- | -------------------------- | --------- |
| GET    | `/me`                     | 내 정보 조회               | ✅        |
| PATCH  | `/me`                     | 내 정보 수정               | ✅        |
| DELETE | `/me`                     | 계정 탈퇴                  | ✅        |
| POST   | `/me/password`            | 비밀번호 변경              | ✅        |
| POST   | `/me/set-password`        | 비밀번호 설정 (소셜 전용)  | ✅        |
| POST   | `/me/verify-password`     | 비밀번호 확인              | ✅        |
| GET    | `/search`                 | 사용자 검색 (페이지네이션) | ✅        |
| GET    | `/email/{email}`          | 이메일로 조회              | ✅        |
| GET    | `/{userId}`               | ID로 조회                  | ✅        |
| GET    | `/status/{status}`        | 상태별 조회                | ✅        |
| PUT    | `/{userId}`               | 사용자 정보 수정           | ✅        |
| PATCH  | `/{userId}/role`          | 권한 변경                  | ✅        |
| PATCH  | `/{userId}/suspend`       | 계정 정지                  | ✅        |
| PATCH  | `/{userId}/activate`      | 계정 활성화                | ✅        |
| DELETE | `/{userId}`               | 소프트 삭제                | ✅        |
| DELETE | `/{userId}/hard`          | 영구 삭제                  | ✅        |

#### Health Check

| Method | Endpoint           | 설명                     |
| ------ | ------------------ | ------------------------ |
| GET    | `/api/health`      | 서비스 헬스 체크         |
| GET    | `/actuator/health` | Spring Actuator 헬스 체크 |
| GET    | `/actuator/info`   | 애플리케이션 정보        |

더 자세한 API 문서는 Postman 컬렉션을 참고하세요.

## 환경 변수

### 데이터베이스

| 변수          | 설명              | 기본값          |
| ------------- | ----------------- | --------------- |
| `DB_HOST`     | MySQL 호스트      | localhost       |
| `DB_PORT`     | MySQL 포트        | 3307            |
| `DB_NAME`     | 데이터베이스 이름 | passit_db       |
| `DB_USER`     | DB 사용자         | passit_user     |
| `DB_PASSWORD` | DB 비밀번호       | passit_password |

### 이메일 (SMTP)

| 변수            | 설명                 | 기본값                    |
| --------------- | -------------------- | ------------------------- |
| `MAIL_USERNAME` | SMTP 사용자 (이메일) | your-email@gmail.com      |
| `MAIL_PASSWORD` | SMTP 비밀번호        | your-app-password         |

Gmail 사용 시 [앱 비밀번호](https://support.google.com/accounts/answer/185833)를 발급받아야 합니다.

### 카카오 OAuth

| 변수                   | 설명                  | 기본값                                          |
| ---------------------- | --------------------- | ----------------------------------------------- |
| `KAKAO_REST_API_KEY`   | 카카오 REST API 키    | -                                               |
| `KAKAO_CLIENT_SECRET`  | 카카오 Client Secret  | -                                               |
| `KAKAO_REDIRECT_URI`   | OAuth 리다이렉트 URI  | http://localhost:8081/api/auth/kakao/callback   |
| `FRONTEND_URL`         | 프론트엔드 URL        | http://localhost:3000                           |

카카오 개발자 콘솔에서 애플리케이션을 등록하고 위 정보를 발급받아야 합니다.

### 관리자 계정

| 변수              | 설명              | 기본값           |
| ----------------- | ----------------- | ---------------- |
| `ADMIN_EMAIL`     | 관리자 이메일     | admin@passit.com |
| `ADMIN_PASSWORD`  | 관리자 비밀번호   | admin123!        |
| `ADMIN_NAME`      | 관리자 이름       | 관리자           |
| `ADMIN_NICKNAME`  | 관리자 닉네임     | admin            |

## 프로젝트 구조

```
service-account/
├── src/
│   ├── main/
│   │   ├── java/com/company/account/
│   │   │   ├── config/              # 설정 클래스 (Security, CORS 등)
│   │   │   ├── controller/          # REST API 컨트롤러
│   │   │   ├── dto/                 # 요청/응답 DTO
│   │   │   ├── entity/              # JPA 엔티티
│   │   │   ├── exception/           # 커스텀 예외 및 핸들러
│   │   │   ├── repository/          # JPA Repository
│   │   │   ├── security/            # JWT, UserDetails 등
│   │   │   ├── service/             # 비즈니스 로직
│   │   │   └── AccountApplication.java
│   │   └── resources/
│   │       ├── application.yml      # 애플리케이션 설정
│   │       └── data.sql             # 초기 데이터 (옵션)
│   └── test/
│       ├── java/                    # 테스트 코드
│       │   ├── unit/               # 단위 테스트
│       │   └── integration/        # 통합 테스트
│       └── resources/
├── build.gradle                     # Gradle 빌드 설정
├── docker-compose.yml               # Docker Compose 설정
└── README.md
```

## 개발 규칙

### 코드 스타일

- Java Code Convention 준수
- Lombok 적극 활용
- Layer 간 의존성 방향: Controller → Service → Repository
- DTO 사용으로 Entity 노출 방지
- 모든 API 응답은 `ApiResponse` 래퍼 사용

### 브랜치 전략

- `main`: 운영 환경 배포 브랜치
- `develop`: 개발 환경 통합 브랜치
- `feature/{기능명}`: 기능 개발 브랜치
- `bugfix/{버그명}`: 버그 수정 브랜치

### 커밋 메시지 규칙

```
feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
style: 코드 포맷팅
refactor: 코드 리팩토링
test: 테스트 코드
chore: 빌드, 설정 파일 수정
```

### Pull Request 규칙

1. PR 생성 시 템플릿 작성
2. 최소 1명 이상 리뷰어 지정
3. CI 성공 필수
4. 승인 후 Squash Merge

## 트러블슈팅

### 빌드 실패

```bash
# Gradle 캐시 삭제
./gradlew clean

# Gradle Wrapper 재설치
./gradlew wrapper
```

### 데이터베이스 연결 실패

```bash
# Docker 컨테이너 상태 확인
docker-compose ps

# MySQL 로그 확인
docker-compose logs mysql

# 컨테이너 재시작
docker-compose restart mysql
```

### 이메일 전송 실패

- Gmail 사용 시 앱 비밀번호를 올바르게 설정했는지 확인
- 2단계 인증이 활성화되어 있는지 확인
- SMTP 포트 (587) 방화벽 확인

### 카카오 로그인 실패

- 카카오 개발자 콘솔에서 Redirect URI가 정확히 등록되었는지 확인
- REST API 키와 Client Secret이 올바른지 확인
- 프론트엔드 URL이 정확히 설정되었는지 확인

## 라이선스

MIT License

## 문의

프로젝트 관련 문의사항은 팀 PM에게 연락하세요.
