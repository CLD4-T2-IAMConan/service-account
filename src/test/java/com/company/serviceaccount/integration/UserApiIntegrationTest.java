package com.company.serviceaccount.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * User API 통합 테스트
 *
 * Testcontainers를 사용하여 실제 MySQL과 연동 테스트
 *
 * 테스트 범위:
 * - 회원가입 전체 플로우
 * - 로그인 플로우
 * - 이메일 인증 플로우
 * - 프로필 관리
 * - 인증/인가 검증
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = com.company.account.accountApplication.class
)
@Testcontainers
@DisplayName("User API 통합 테스트")
class UserApiIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true); // 성능 향상을 위해 컨테이너 재사용

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MySQL Testcontainers 설정
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        // 관리자 계정 설정
        registry.add("app.admin.email", () -> "admin@test.com");
        registry.add("app.admin.password", () -> "admin123!");
        registry.add("app.admin.name", () -> "테스트 관리자");
        registry.add("app.admin.nickname", () -> "admin");
        // 카카오 로그인 설정 (테스트용)
        registry.add("kakao.rest-api-key", () -> "test-rest-api-key");
        registry.add("kakao.client-secret", () -> "test-client-secret");
        registry.add("kakao.admin-key", () -> "test-admin-key");
        registry.add("kakao.redirect-uri", () -> "http://localhost:8081/api/auth/kakao/callback");
        registry.add("kakao.frontend-url", () -> "http://localhost:3000");
    }

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    @DisplayName("회원가입 성공 - 정상적인 데이터")
    void signup_success() {
        String email = "signup-" + System.currentTimeMillis() + "@example.com";

        Map<String, Object> request = Map.of(
            "email", email,
            "password", "Password123!",
            "name", "회원가입테스터"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/auth/signup")
        .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.userId", notNullValue())
            .body("data.email", equalTo(email))
            .body("data.name", equalTo("회원가입테스터"))
            .body("data.role", equalTo("USER"));
    }

    @Test
    @DisplayName("중복 이메일로 회원가입 시도 - 실패")
    void signup_duplicateEmail_fails() {
        String email = "duplicate-" + System.currentTimeMillis() + "@example.com";

        // Given - 첫 번째 사용자 생성
        Map<String, Object> firstUser = Map.of(
            "email", email,
            "password", "Password123!",
            "name", "첫번째"
        );

        given()
            .contentType(ContentType.JSON)
            .body(firstUser)
        .when()
            .post("/auth/signup")
        .then()
            .statusCode(201);

        // When & Then - 같은 이메일로 다시 시도
        Map<String, Object> secondUser = Map.of(
            "email", email,
            "password", "DifferentPass123!",
            "name", "두번째"
        );

        given()
            .contentType(ContentType.JSON)
            .body(secondUser)
        .when()
            .post("/auth/signup")
        .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("message", containsString("이미 존재하는 이메일"));
    }

    @Test
    @DisplayName("잘못된 이메일 형식 - 실패")
    void signup_invalidEmail_fails() {
        Map<String, Object> request = Map.of(
            "email", "invalid-email",
            "password", "Password123!",
            "name", "테스터"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/auth/signup")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("짧은 비밀번호 - 실패")
    void signup_shortPassword_fails() {
        Map<String, Object> request = Map.of(
            "email", "test@example.com",
            "password", "short",
            "name", "테스터"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/auth/signup")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        String email = "login-" + System.currentTimeMillis() + "@example.com";

        // Given - 사용자 생성
        Map<String, Object> signupRequest = Map.of(
            "email", email,
            "password", "Password123!",
            "name", "로그인테스터"
        );

        given()
            .contentType(ContentType.JSON)
            .body(signupRequest)
        .when()
            .post("/auth/signup");

        // When - 로그인
        Map<String, Object> loginRequest = Map.of(
            "email", email,
            "password", "Password123!"
        );

        given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.accessToken", notNullValue())
            .body("data.refreshToken", notNullValue())
            .body("data.email", equalTo(email));
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시도 - 실패")
    void login_wrongPassword_fails() {
        String email = "wrongpwd-" + System.currentTimeMillis() + "@example.com";

        // Given - 사용자 생성
        Map<String, Object> signupRequest = Map.of(
            "email", email,
            "password", "Password123!",
            "name", "로그인테스터"
        );

        given()
            .contentType(ContentType.JSON)
            .body(signupRequest)
        .when()
            .post("/auth/signup");

        // When & Then - 잘못된 비밀번호로 로그인
        Map<String, Object> loginRequest = Map.of(
            "email", email,
            "password", "WrongPassword123!"
        );

        given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("message", containsString("이메일 또는 비밀번호"));
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 - 실패")
    void login_nonexistentEmail_fails() {
        Map<String, Object> loginRequest = Map.of(
            "email", "nonexistent@example.com",
            "password", "Password123!"
        );

        given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("message", containsString("이메일 또는 비밀번호"));
    }

    @Test
    @DisplayName("인증 없이 보호된 리소스 접근 - 실패")
    void accessProtectedResource_withoutAuth_fails() {
        given()
        .when()
            .get("/users/me")
        .then()
            .statusCode(anyOf(equalTo(401), equalTo(403))); // Spring Security는 401 또는 403을 반환할 수 있음
    }

    @Test
    @DisplayName("잘못된 토큰으로 접근 - 실패")
    void accessProtectedResource_withInvalidToken_fails() {
        String invalidToken = "invalid.jwt.token";

        given()
            .header("Authorization", "Bearer " + invalidToken)
        .when()
            .get("/users/me")
        .then()
            .statusCode(anyOf(equalTo(401), equalTo(403))); // Spring Security는 401 또는 403을 반환할 수 있음
    }

    @Test
    @DisplayName("프로필 조회 - 성공")
    void getProfile_success() {
        String email = "profile-" + System.currentTimeMillis() + "@example.com";

        // Given - 사용자 생성 및 로그인
        Map<String, Object> signupRequest = Map.of(
            "email", email,
            "password", "Password123!",
            "name", "프로필테스터"
        );

        given()
            .contentType(ContentType.JSON)
            .body(signupRequest)
        .when()
            .post("/auth/signup");

        Map<String, Object> loginRequest = Map.of(
            "email", email,
            "password", "Password123!"
        );

        String accessToken = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("data.accessToken");

        // When & Then
        given()
            .header("Authorization", "Bearer " + accessToken)
        .when()
            .get("/users/me")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.email", equalTo(email))
            .body("data.name", equalTo("프로필테스터"));
    }

    @Test
    @DisplayName("프로필 업데이트 - 성공")
    void updateProfile_success() {
        String email = "update-" + System.currentTimeMillis() + "@example.com";

        // Given - 사용자 생성 및 로그인
        Map<String, Object> signupRequest = Map.of(
            "email", email,
            "password", "Password123!",
            "name", "업데이트전"
        );

        given()
            .contentType(ContentType.JSON)
            .body(signupRequest)
        .when()
            .post("/auth/signup");

        Map<String, Object> loginRequest = Map.of(
            "email", email,
            "password", "Password123!"
        );

        String accessToken = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("data.accessToken");

        // When - 프로필 업데이트
        Map<String, Object> updateRequest = Map.of(
            "name", "업데이트후",
            "phone", "010-1234-5678"
        );

        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType(ContentType.JSON)
            .body(updateRequest)
        .when()
            .patch("/users/me")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.name", equalTo("업데이트후"))
            .body("data.phone", equalTo("010-1234-5678"));
    }

    @Test
    @DisplayName("비밀번호 변경 - 성공")
    void changePassword_success() {
        String email = "changepwd-" + System.currentTimeMillis() + "@example.com";

        // Given - 사용자 생성 및 로그인
        Map<String, Object> signupRequest = Map.of(
            "email", email,
            "password", "OldPassword123!",
            "name", "비밀번호변경테스터"
        );

        given()
            .contentType(ContentType.JSON)
            .body(signupRequest)
        .when()
            .post("/auth/signup");

        Map<String, Object> loginRequest = Map.of(
            "email", email,
            "password", "OldPassword123!"
        );

        String accessToken = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("data.accessToken");

        // When - 비밀번호 변경
        Map<String, Object> changePasswordRequest = Map.of(
            "oldPassword", "OldPassword123!",
            "newPassword", "NewPassword123!"
        );

        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType(ContentType.JSON)
            .body(changePasswordRequest)
        .when()
            .post("/users/me/password")
        .then()
            .statusCode(200)
            .body("success", equalTo(true));

        // Then - 새 비밀번호로 로그인 시도
        Map<String, Object> newLoginRequest = Map.of(
            "email", email,
            "password", "NewPassword123!"
        );

        given()
            .contentType(ContentType.JSON)
            .body(newLoginRequest)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.accessToken", notNullValue());
    }
}
