# Passit Service Account API 문서

## 📌 목차
1. [인증 API](#인증-api)
2. [사용자 관리 API](#사용자-관리-api)
3. [사용자 검색 및 페이지네이션](#사용자-검색-및-페이지네이션)
4. [에러 응답](#에러-응답)
5. [인증 플로우](#인증-플로우)

---

## 🔐 인증 API

### 1. 회원가입
새로운 사용자를 등록합니다. 회원가입 시 자동으로 이메일 인증 코드가 전송됩니다.

**Endpoint:** `POST /api/auth/signup`

**Request Body:**
```json
{
  "email": "test@example.com",
  "password": "password123",
  "name": "홍길동",
  "nickname": "길동이"
}
```

**Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "test@example.com",
    "name": "홍길동",
    "nickname": "길동이",
    "role": "USER",
    "status": "ACTIVE",
    "emailVerified": false,
    "createdAt": "2025-12-08T21:00:00",
    "updatedAt": "2025-12-08T21:00:00"
  },
  "message": "회원가입이 완료되었습니다. 이메일 인증을 진행해주세요."
}
```

**로컬 환경:** 서버 콘솔에 6자리 인증 코드가 출력됩니다.

---

### 2. 이메일 인증 코드 전송
이메일 인증 코드를 전송하거나 재전송합니다.

**Endpoint:** `POST /api/auth/send-verification-code`

**Request Body:**
```json
{
  "email": "test@example.com"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "email": "test@example.com",
    "expiresAt": "2025-12-08T21:10:00"
  },
  "message": "인증 코드가 전송되었습니다"
}
```

**유효 시간:** 10분

---

### 3. 이메일 인증 코드 검증
이메일 인증 코드를 검증하고 이메일 인증을 완료합니다.

**Endpoint:** `POST /api/auth/verify-email`

**Request Body:**
```json
{
  "email": "test@example.com",
  "code": "123456"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": null,
  "message": "이메일 인증이 완료되었습니다"
}
```

---

### 4. 로그인
이메일과 비밀번호로 로그인하고 JWT 토큰을 발급받습니다.

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "email": "test@example.com",
  "password": "password123"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "test@example.com",
    "name": "홍길동",
    "role": "USER",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresAt": "2025-12-08T22:00:00"
  },
  "message": "로그인 성공"
}
```

**토큰 유효 시간:**
- Access Token: 1시간
- Refresh Token: 7일

**요구 사항:**
- 이메일 인증 완료 필수
- 계정 상태가 ACTIVE여야 함

---

### 5. Access Token 갱신
Refresh Token으로 새로운 Access Token을 발급받습니다.

**Endpoint:** `POST /api/auth/refresh`

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresAt": "2025-12-08T23:00:00"
  },
  "message": "토큰 갱신 성공"
}
```

---

### 6. 로그아웃
로그아웃하고 Refresh Token을 무효화합니다.

**Endpoint:** `POST /api/auth/logout`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": null,
  "message": "로그아웃 성공"
}
```

---

## 👥 사용자 관리 API

### 1. 사용자 ID로 조회
특정 사용자를 ID로 조회합니다.

**Endpoint:** `GET /api/users/{userId}`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "test@example.com",
    "name": "홍길동",
    "nickname": "길동이",
    "role": "USER",
    "status": "ACTIVE",
    "emailVerified": true,
    "emailVerifiedAt": "2025-12-08T21:05:00",
    "lastLoginAt": "2025-12-08T21:10:00",
    "createdAt": "2025-12-08T21:00:00",
    "updatedAt": "2025-12-08T21:10:00"
  },
  "message": null
}
```

---

### 2. 이메일로 사용자 조회
이메일로 사용자를 조회합니다.

**Endpoint:** `GET /api/users/email/{email}`

**Example:** `GET /api/users/email/test@example.com`

---

### 3. 전체 사용자 조회
모든 사용자를 조회합니다.

**Endpoint:** `GET /api/users`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "userId": 1,
      "email": "test@example.com",
      "name": "홍길동",
      ...
    },
    {
      "userId": 2,
      "email": "admin@example.com",
      "name": "관리자",
      ...
    }
  ],
  "message": null
}
```

---

### 4. 상태별 사용자 조회
특정 상태의 사용자만 조회합니다.

**Endpoint:** `GET /api/users/status/{status}`

**Status 값:**
- `ACTIVE` - 활성
- `SUSPENDED` - 정지
- `DELETED` - 삭제

**Example:** `GET /api/users/status/ACTIVE`

---

### 5. 사용자 정보 수정
사용자의 이름, 닉네임, 프로필 이미지 등을 수정합니다.

**Endpoint:** `PUT /api/users/{userId}`

**Request Body:**
```json
{
  "name": "김철수",
  "nickname": "철수왕",
  "profileImageUrl": "https://example.com/profile.jpg"
}
```

**Response:** `200 OK`

---

### 6. 사용자 권한 변경
사용자의 권한을 변경합니다.

**Endpoint:** `PATCH /api/users/{userId}/role`

**Request Body:**
```json
{
  "role": "ADMIN"
}
```

**Role 값:**
- `USER` - 일반 사용자
- `ADMIN` - 관리자

---

### 7. 사용자 정지
사용자를 정지 상태로 변경합니다.

**Endpoint:** `PATCH /api/users/{userId}/suspend`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "status": "SUSPENDED",
    ...
  },
  "message": "사용자가 정지되었습니다"
}
```

---

### 8. 사용자 활성화 (재활성화)
정지되거나 삭제된 사용자를 다시 활성화합니다.

**Endpoint:** `PATCH /api/users/{userId}/activate`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "status": "ACTIVE",
    "deletedAt": null,
    ...
  },
  "message": "사용자가 활성화되었습니다"
}
```

---

### 9. 사용자 소프트 삭제
사용자를 소프트 삭제합니다. DB에서 삭제되지 않고 상태만 변경됩니다.

**Endpoint:** `DELETE /api/users/{userId}`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": null,
  "message": "사용자가 삭제되었습니다"
}
```

**변경 사항:**
- `status` → `DELETED`
- `deletedAt` → 현재 시각

**복구 가능:** `/api/users/{userId}/activate` 로 재활성화 가능

---

### 10. 사용자 하드 삭제 (영구 삭제)
사용자를 DB에서 완전히 삭제합니다.

**Endpoint:** `DELETE /api/users/{userId}/hard`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": null,
  "message": "사용자가 영구 삭제되었습니다"
}
```

**⚠️ 주의:** 복구 불가능합니다.

---

## 🔍 사용자 검색 및 페이지네이션

### 사용자 검색
검색, 필터링, 정렬, 페이지네이션 기능을 제공합니다.

**Endpoint:** `GET /api/users/search`

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| keyword | String | No | - | 검색어 (이름, 이메일, 닉네임) |
| status | Enum | No | - | 상태 필터 (ACTIVE, SUSPENDED, DELETED) |
| page | Integer | No | 0 | 페이지 번호 (0부터 시작) |
| size | Integer | No | 20 | 페이지 크기 |
| sortBy | String | No | createdAt | 정렬 기준 (createdAt, userId, name, email) |
| sortDirection | String | No | DESC | 정렬 방향 (ASC, DESC) |

**Examples:**

1. **전체 사용자 페이지네이션:**
   ```
   GET /api/users/search?page=0&size=10
   ```

2. **키워드로 검색:**
   ```
   GET /api/users/search?keyword=홍길동&page=0&size=10
   ```

3. **상태별 + 키워드 검색:**
   ```
   GET /api/users/search?keyword=test&status=ACTIVE&page=0&size=10
   ```

4. **이름으로 정렬 (오름차순):**
   ```
   GET /api/users/search?sortBy=name&sortDirection=ASC&page=0&size=10
   ```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "userId": 1,
        "email": "test@example.com",
        "name": "홍길동",
        ...
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": {
        "sorted": true,
        "unsorted": false,
        "empty": false
      },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalPages": 5,
    "totalElements": 50,
    "last": false,
    "first": true,
    "size": 10,
    "number": 0,
    "numberOfElements": 10,
    "empty": false
  },
  "message": null
}
```

---

## ❌ 에러 응답

모든 에러는 다음 형식으로 반환됩니다:

```json
{
  "success": false,
  "data": null,
  "message": "에러 메시지"
}
```

**주요 에러 메시지:**

| HTTP Status | Message | 설명 |
|-------------|---------|------|
| 400 Bad Request | "이미 존재하는 이메일입니다" | 중복 이메일 |
| 400 Bad Request | "이미 존재하는 닉네임입니다" | 중복 닉네임 |
| 400 Bad Request | "유효하지 않은 인증 코드입니다" | 잘못된 인증 코드 |
| 400 Bad Request | "인증 코드가 만료되었습니다" | 10분 경과 |
| 400 Bad Request | "이메일 또는 비밀번호가 올바르지 않습니다" | 로그인 실패 |
| 400 Bad Request | "이메일 인증이 필요합니다" | 미인증 사용자 로그인 시도 |
| 400 Bad Request | "삭제된 계정입니다" | 삭제된 계정 로그인 시도 |
| 400 Bad Request | "정지된 계정입니다" | 정지된 계정 로그인 시도 |
| 400 Bad Request | "유효하지 않은 Refresh Token입니다" | 토큰 갱신 실패 |
| 404 Not Found | "사용자를 찾을 수 없습니다" | 존재하지 않는 사용자 |

---

## 🔄 인증 플로우

### 회원가입 → 로그인 전체 플로우

```
1. 회원가입
   POST /api/auth/signup
   └─> 이메일 인증 코드 자동 전송 (콘솔 출력)

2. 콘솔에서 6자리 인증 코드 확인
   ===================================================
   📧 [이메일 인증 코드 전송]
   수신자: test@example.com
   인증 코드: 123456
   유효 시간: 10분
   ===================================================

3. 이메일 인증
   POST /api/auth/verify-email
   {
     "email": "test@example.com",
     "code": "123456"
   }
   └─> 환영 이메일 전송 (콘솔 출력)

4. 로그인
   POST /api/auth/login
   {
     "email": "test@example.com",
     "password": "password123"
   }
   └─> JWT 토큰 발급

5. 인증이 필요한 API 호출
   GET /api/users/1
   Headers: Authorization: Bearer {accessToken}

6. Access Token 만료 시 갱신
   POST /api/auth/refresh
   {
     "refreshToken": "{refreshToken}"
   }

7. 로그아웃
   POST /api/auth/logout
   Headers: Authorization: Bearer {accessToken}
```

---

## 🔧 JWT 토큰 사용

### Access Token
모든 인증이 필요한 API 호출 시 헤더에 포함:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Token Payload
```json
{
  "sub": "1",           // userId
  "email": "test@example.com",
  "role": "USER",
  "iat": 1702000000,    // 발급 시간
  "exp": 1702003600     // 만료 시간 (1시간 후)
}
```

---

## 📝 Postman 컬렉션

두 개의 Postman 컬렉션 파일이 제공됩니다:

1. **Passit-Auth-API.postman_collection.json**
   - 인증 관련 API (회원가입, 로그인, 이메일 인증 등)
   - 전체 플로우 테스트 포함

2. **Passit-ServiceAccount-API.postman_collection.json**
   - 사용자 관리 CRUD API
   - 검색, 페이지네이션, 상태 관리 등

### 환경 변수
```json
{
  "baseUrl": "http://localhost:8081",
  "userEmail": "test@example.com",
  "userId": "1",
  "accessToken": "",
  "refreshToken": ""
}
```

---

## 🚀 빠른 시작

### 1. 서비스 실행
```bash
cd service-account
./gradlew bootRun
```

### 2. Health Check
```bash
curl http://localhost:8081/actuator/health
```

### 3. 회원가입 테스트
```bash
curl -X POST http://localhost:8081/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "name": "홍길동",
    "nickname": "길동이"
  }'
```

### 4. 콘솔에서 인증 코드 확인
서버 콘솔 로그를 확인하여 6자리 코드를 복사합니다.

### 5. 이메일 인증
```bash
curl -X POST http://localhost:8081/api/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "code": "123456"
  }'
```

### 6. 로그인
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

---

## 📌 참고 사항

### 로컬 개발 환경
- 이메일은 실제로 전송되지 않고 **서버 콘솔**에 출력됩니다
- 인증 코드는 **10분간 유효**합니다
- Access Token은 **1시간**, Refresh Token은 **7일** 유효합니다

### 프로덕션 환경
- `LocalEmailService`를 `AwsSesEmailService`로 교체하여 실제 이메일 전송
- JWT Secret Key를 환경변수로 관리
- HTTPS 사용 필수
- Rate Limiting 적용 권장

---

## 📧 문의
이슈가 있으시면 GitHub Issues로 문의해주세요.
