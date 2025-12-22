package com.company.account.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenCache implements Serializable {
    private Long userId;
    private String token;
    private Boolean isValid;
    private LocalDateTime expiredAt;
}
