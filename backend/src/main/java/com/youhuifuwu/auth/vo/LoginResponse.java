package com.youhuifuwu.auth.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private Long accountId;
    private String role;
    private String displayName;
    private String token;
    private Boolean profileCompleted;
    private Boolean passwordConfigured;
    private Boolean needProfileCompletion;
}
