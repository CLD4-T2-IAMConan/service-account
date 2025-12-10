package com.company.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class KakaoAuthRequest {

    /**
     * 카카오 콜백 요청 (인증 코드 포함)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Callback {
        private String code;
        private String error;
        private String error_description;
    }
}
