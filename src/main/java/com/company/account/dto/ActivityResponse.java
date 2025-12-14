package com.company.account.dto;

import com.company.account.entity.Activity;
import com.company.account.entity.Activity.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponse {
    private Long activityId;
    private Long userId;
    private Long relatedUserId;
    private ActivityType activityType;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    public static ActivityResponse fromEntity(Activity activity) {
        return ActivityResponse.builder()
                .activityId(activity.getActivityId())
                .userId(activity.getUserId())
                .relatedUserId(activity.getRelatedUserId())
                .activityType(activity.getActivityType())
                .rating(activity.getRating())
                .comment(activity.getComment())
                .createdAt(activity.getCreatedAt())
                .build();
    }
}
