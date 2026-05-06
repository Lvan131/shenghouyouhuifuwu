package com.youhuifuwu.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminUserSaveRequest {

    @NotBlank(message = "Real name is required")
    private String realName;

    @NotBlank(message = "User number is required")
    private String userNo;

    @NotBlank(message = "User type is required")
    @Pattern(regexp = "STUDENT|TEACHER", message = "User type must be STUDENT or TEACHER")
    private String userType;

    private String phone;

    private String avatarUrl;

    private String password;

    @NotNull(message = "Status is required")
    @Min(value = 0, message = "Status must be 0 or 1")
    @Max(value = 1, message = "Status must be 0 or 1")
    private Integer status;
}
