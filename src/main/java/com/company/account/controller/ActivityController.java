package com.company.account.controller;

import com.company.account.dto.ActivityRequest;
import com.company.account.dto.ActivityResponse;
import com.company.account.dto.ApiResponse;
import com.company.account.entity.Activity.ActivityType;
import com.company.account.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /**
     * 활동 내역 생성
     * POST /api/activities
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ActivityResponse>> createActivity(
            @Valid @RequestBody ActivityRequest.Create request,
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        log.info("Request to create activity for user ID: {}", userId);

        ActivityResponse response = activityService.createActivity(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "활동 내역이 기록되었습니다"));
    }

    /**
     * 내 활동 내역 조회
     * GET /api/activities/me?page=0&size=20&type=PURCHASE
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<ActivityResponse>>> getMyActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ActivityType type,
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        log.info("Request to get activities for user ID: {}, page: {}, size: {}, type: {}", 
                userId, page, size, type);

        Page<ActivityResponse> response = activityService.getMyActivities(userId, page, size, type);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 최근 활동 내역 조회 (최대 10개)
     * GET /api/activities/me/recent
     */
    @GetMapping("/me/recent")
    public ResponseEntity<ApiResponse<java.util.List<ActivityResponse>>> getRecentActivities(
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        log.info("Request to get recent activities for user ID: {}", userId);

        java.util.List<ActivityResponse> response = activityService.getRecentActivities(userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 활동 내역 통계
     * GET /api/activities/me/stats
     */
    @GetMapping("/me/stats")
    public ResponseEntity<ApiResponse<ActivityService.ActivityStats>> getActivityStats(
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        log.info("Request to get activity stats for user ID: {}", userId);

        ActivityService.ActivityStats response = activityService.getActivityStats(userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
