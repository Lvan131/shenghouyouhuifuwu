package com.youhuifuwu.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserRegisterRequest {

    @NotBlank(message = "realName cannot be blank")
    private String realName;

    @NotBlank(message = "userNo cannot be blank")
    private String userNo;

    @NotBlank(message = "userType cannot be blank")
    @Pattern(regexp = "STUDENT|TEACHER", message = "userType must be STUDENT or TEACHER")
    private String userType;

    private String phone;

    private String avatarUrl;

    @NotBlank(message = "password cannot be blank")
    private String password;
}
