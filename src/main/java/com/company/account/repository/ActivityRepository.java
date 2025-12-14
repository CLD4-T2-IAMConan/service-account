package com.company.account.repository;

import com.company.account.entity.Activity;
import com.company.account.entity.Activity.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    // 사용자별 활동 내역 조회 (최신순)
    Page<Activity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 사용자별 활동 타입별 조회
    Page<Activity> findByUserIdAndActivityTypeOrderByCreatedAtDesc(
            Long userId, ActivityType activityType, Pageable pageable);

    // 사용자별 활동 내역 개수
    long countByUserId(Long userId);

    // 사용자별 활동 타입별 개수
    long countByUserIdAndActivityType(Long userId, ActivityType activityType);

    // 최근 활동 내역 조회 (페이지네이션 없이)
    List<Activity> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
