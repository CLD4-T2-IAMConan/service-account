package com.company.account.controller;

import com.company.account.dto.ApiResponse;
import com.company.account.dto.UserRequest;
import com.company.account.dto.UserResponse;
import com.company.account.entity.User.UserStatus;
import com.company.account.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 사용자 생성
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserRequest.Create request) {
        log.info("Request to create user with email: {}", request.getEmail());

        UserResponse response = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "사용자가 성공적으로 생성되었습니다"));
    }

    /**
     * 사용자 검색 및 페이지네이션 조회
     * GET /api/users/search
     * @param keyword 검색어 (이름, 이메일, 닉네임) - 선택 사항
     * @param status 상태 필터 (ACTIVE, SUSPENDED, DELETED) - 선택 사항
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @param sortBy 정렬 기준 (기본값: createdAt)
     * @param sortDirection 정렬 방향 (ASC/DESC, 기본값: DESC)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("Request to search users - keyword: {}, status: {}, page: {}, size: {}",
                keyword, status, page, size);

        Page<UserResponse> response = userService.searchUsersWithPagination(
                keyword, status, page, size, sortBy, sortDirection);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 이메일로 사용자 조회
     * GET /api/users/email/{email}
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(@PathVariable String email) {
        log.info("Request to get user by email: {}", email);

        UserResponse response = userService.getUserByEmail(email);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 전체 사용자 조회
     * GET /api/users
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        log.info("Request to get all users");

        List<UserResponse> response = userService.getAllUsers();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 ID로 조회
     * GET /api/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        log.info("Request to get user by ID: {}", userId);

        UserResponse response = userService.getUserById(userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 상태별 사용자 조회
     * GET /api/users/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByStatus(
            @PathVariable UserStatus status) {
        log.info("Request to get users by status: {}", status);

        List<UserResponse> response = userService.getUsersByStatus(status);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 정보 수정
     * PUT /api/users/{userId}
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserRequest.Update request) {
        log.info("Request to update user with ID: {}", userId);

        UserResponse response = userService.updateUser(userId, request);

        return ResponseEntity.ok(ApiResponse.success(response, "사용자 정보가 수정되었습니다"));
    }

    /**
     * 사용자 권한 변경
     * PATCH /api/users/{userId}/role
     */
    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UserRequest.UpdateRole request) {
        log.info("Request to update role for user with ID: {}", userId);

        UserResponse response = userService.updateUserRole(userId, request);

        return ResponseEntity.ok(ApiResponse.success(response, "사용자 권한이 변경되었습니다"));
    }

    /**
     * 사용자 정지
     * PATCH /api/users/{userId}/suspend
     */
    @PatchMapping("/{userId}/suspend")
    public ResponseEntity<ApiResponse<UserResponse>> suspendUser(@PathVariable Long userId) {
        log.info("Request to suspend user with ID: {}", userId);

        UserResponse response = userService.suspendUser(userId);

        return ResponseEntity.ok(ApiResponse.success(response, "사용자가 정지되었습니다"));
    }

    /**
     * 사용자 활성화
     * PATCH /api/users/{userId}/activate
     */
    @PatchMapping("/{userId}/activate")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable Long userId) {
        log.info("Request to activate user with ID: {}", userId);

        UserResponse response = userService.activateUser(userId);

        return ResponseEntity.ok(ApiResponse.success(response, "사용자가 활성화되었습니다"));
    }

    /**
     * 사용자 소프트 삭제 (탈퇴)
     * DELETE /api/users/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        log.info("Request to soft delete user with ID: {}", userId);

        userService.deleteUser(userId);

        return ResponseEntity.ok(ApiResponse.success(null, "사용자가 삭제되었습니다"));
    }

    /**
     * 사용자 하드 삭제 (영구 삭제)
     * DELETE /api/users/{userId}/hard
     */
    @DeleteMapping("/{userId}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDeleteUser(@PathVariable Long userId) {
        log.info("Request to hard delete user with ID: {}", userId);

        userService.hardDeleteUser(userId);

        return ResponseEntity.ok(ApiResponse.success(null, "사용자가 영구 삭제되었습니다"));
    }
}
