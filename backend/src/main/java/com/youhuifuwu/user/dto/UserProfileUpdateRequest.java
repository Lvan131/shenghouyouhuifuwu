package com.youhuifuwu.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {

    private String realName;

    @NotBlank(message = "userNo cannot be blank")
    private String userNo;

    @NotBlank(message = "userType cannot be blank")
    private String userType;

    private String phone;

    private String avatarUrl;
}

