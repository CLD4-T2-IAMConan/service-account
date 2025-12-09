# Docker MySQL 연동 가이드

이 문서는 Passit Service Account 프로젝트에서 Docker를 사용하여 MySQL 데이터베이스를 연동하는 방법을 설명합니다.

## 목차
1. [사전 요구사항](#사전-요구사항)
2. [Docker Compose 설정](#docker-compose-설정)
3. [환경 변수 설정](#환경-변수-설정)
4. [데이터베이스 초기화](#데이터베이스-초기화)
5. [실행 방법](#실행-방법)
6. [연결 확인](#연결-확인)
7. [문제 해결](#문제-해결)

## 사전 요구사항

- Docker 및 Docker Compose 설치
- Java 17 이상
- Gradle

## Docker Compose 설정

프로젝트는 현재 PostgreSQL을 사용하도록 설정되어 있지만, MySQL로 변경하려면 `docker-compose.yml` 파일을 수정해야 합니다.

### MySQL용 docker-compose.yml 예시

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: account-mysql
    environment:
      MYSQL_DATABASE: passit_db
      MYSQL_USER: passit_user
      MYSQL_PASSWORD: passit_password
      MYSQL_ROOT_PASSWORD: root_password
    ports:
      - "3307:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./migration.sql:/docker-entrypoint-initdb.d/init.sql
    command: --default-authentication-plugin=mysql_native_password
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-proot_password"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: account-service
    environment:
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: passit_db
      DB_USER: passit_user
      DB_PASSWORD: passit_password
    ports:
      - "8081:8081"
    depends_on:
      mysql:
        condition: service_healthy

volumes:
  mysql_data:
```

### 주요 설정 설명

- **image**: `mysql:8.0` - MySQL 8.0 공식 이미지 사용
- **ports**: `3307:3306` - 호스트의 3307 포트를 컨테이너의 3306 포트에 매핑 (로컬 MySQL과 충돌 방지)
- **volumes**:
  - `mysql_data` - 데이터 영속성 보장
  - `migration.sql` - 컨테이너 시작 시 자동으로 실행될 초기화 스크립트
- **healthcheck**: MySQL이 완전히 준비될 때까지 대기

## 환경 변수 설정

### 1. .env 파일 생성

`.env.example` 파일을 복사하여 `.env` 파일을 생성합니다:

```bash
cp .env.example .env
```

### 2. .env 파일 내용

```properties
# Database Configuration
DB_HOST=localhost
DB_PORT=3307
DB_NAME=passit_db
DB_USER=passit_user
DB_PASSWORD=passit_password

# Application Configuration
SPRING_PROFILES_ACTIVE=default
SERVER_PORT=8081

# Email Configuration (SMTP 프로필 사용 시 필요)
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-16-digit-app-password
```

### 3. application.yml 설정

현재 `src/main/resources/application.yml`은 이미 MySQL 연동을 위해 설정되어 있습니다:

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3307}/${DB_NAME:passit_db}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: ${DB_USER:passit_user}
    password: ${DB_PASSWORD:passit_password}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
```

### 주요 파라미터 설명

- **useSSL=false**: 로컬 개발 환경에서 SSL 비활성화
- **allowPublicKeyRetrieval=true**: MySQL 8.0의 인증 방식 허용
- **serverTimezone=Asia/Seoul**: 타임존 설정
- **ddl-auto=validate**: 스키마 검증만 수행 (자동 생성하지 않음)

## 데이터베이스 초기화

### migration.sql 파일

프로젝트에 포함된 `migration.sql` 파일이 컨테이너 시작 시 자동으로 실행됩니다. 이 파일에는 다음이 포함됩니다:

- 테이블 생성 (users, email_verifications)
- 인덱스 생성
- 초기 데이터 (필요한 경우)

### 스키마 구조

#### users 테이블
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### email_verifications 테이블
```sql
CREATE TABLE email_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    verification_code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_code (verification_code),
    INDEX idx_expires (expires_at)
);
```

## 실행 방법

### 1. Docker만 실행 (MySQL만 시작)

로컬에서 애플리케이션을 개발하면서 MySQL만 Docker로 실행:

```bash
# MySQL 컨테이너만 시작
docker-compose up mysql -d

# 로그 확인
docker-compose logs -f mysql

# MySQL 준비 확인
docker-compose ps
```

### 2. Spring Boot 애플리케이션 실행

```bash
# Gradle로 실행
./gradlew bootRun

# 또는 빌드 후 실행
./gradlew build
java -jar build/libs/template-0.0.1-SNAPSHOT.jar
```

### 3. 전체 스택 실행 (MySQL + App)

Docker Compose로 모든 서비스를 실행:

```bash
# 전체 서비스 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 특정 서비스 로그만 확인
docker-compose logs -f app
```

### 4. 중지 및 정리

```bash
# 서비스 중지
docker-compose down

# 볼륨까지 삭제 (데이터 초기화)
docker-compose down -v
```

## 연결 확인

### 1. MySQL 직접 접속

```bash
# Docker 컨테이너 내부에서 접속
docker exec -it account-mysql mysql -u passit_user -ppassit_password passit_db

# 또는 호스트에서 MySQL 클라이언트로 접속
mysql -h 127.0.0.1 -P 3307 -u passit_user -ppassit_password passit_db
```

### 2. 테이블 확인

```sql
-- 데이터베이스 선택
USE passit_db;

-- 테이블 목록 확인
SHOW TABLES;

-- 테이블 구조 확인
DESCRIBE users;
DESCRIBE email_verifications;

-- 데이터 확인
SELECT * FROM users;
```

### 3. 애플리케이션 Health Check

```bash
# Health endpoint 확인
curl http://localhost:8081/actuator/health

# 응답 예시:
# {
#   "status": "UP",
#   "components": {
#     "db": {
#       "status": "UP",
#       "details": {
#         "database": "MySQL",
#         "validationQuery": "isValid()"
#       }
#     }
#   }
# }
```

## 문제 해결

### 1. 포트 충돌

**문제**: 3307 포트가 이미 사용 중

**해결**:
```bash
# 포트 사용 확인
lsof -i :3307

# docker-compose.yml에서 다른 포트로 변경
ports:
  - "3308:3306"

# .env 파일도 함께 수정
DB_PORT=3308
```

### 2. 연결 거부

**문제**: `Communications link failure`

**해결**:
```bash
# MySQL 컨테이너 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs mysql

# 컨테이너 재시작
docker-compose restart mysql
```

### 3. 인증 실패

**문제**: `Access denied for user`

**해결**:
- `.env` 파일의 DB_USER와 DB_PASSWORD 확인
- `docker-compose.yml`의 환경 변수 확인
- 컨테이너 재생성:
  ```bash
  docker-compose down -v
  docker-compose up -d
  ```

### 4. 타임존 문제

**문제**: 시간이 UTC로 저장됨

**해결**:
- JDBC URL에 `serverTimezone=Asia/Seoul` 파라미터 추가 (이미 설정됨)
- 또는 MySQL 컨테이너 환경 변수에 추가:
  ```yaml
  environment:
    TZ: Asia/Seoul
  ```

### 5. Flyway/Liquibase 마이그레이션 오류

**문제**: 스키마 변경 시 오류 발생

**해결**:
```bash
# 데이터베이스 초기화
docker-compose down -v
docker-compose up -d mysql

# migration.sql 재실행 또는 애플리케이션 재시작
```

### 6. 한글 깨짐 문제

**문제**: 한글 데이터가 깨져서 저장됨

**해결**: docker-compose.yml에 character set 설정 추가
```yaml
mysql:
  command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
```

## 참고 자료

- [MySQL Docker Hub](https://hub.docker.com/_/mysql)
- [Spring Boot Database Connectivity](https://docs.spring.io/spring-boot/docs/current/reference/html/data.html#data.sql.datasource)
- [MySQL Connector/J Documentation](https://dev.mysql.com/doc/connector-j/en/)
- 프로젝트 관련 문서:
  - [API Documentation](./API-DOCUMENTATION.md)
  - [Email Setup Guide](./EMAIL-SETUP-GUIDE.md)
