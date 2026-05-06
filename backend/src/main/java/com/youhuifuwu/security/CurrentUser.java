package com.youhuifuwu.security;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CurrentUser {

    private Long accountId;
    private String role;
    private String displayName;
}

