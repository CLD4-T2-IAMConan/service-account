package com.company.account.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenValidationCache implements Serializable {
    private Long userId;
    private String email;
    private String role;
    private LocalDateTime expiredAt;
}
