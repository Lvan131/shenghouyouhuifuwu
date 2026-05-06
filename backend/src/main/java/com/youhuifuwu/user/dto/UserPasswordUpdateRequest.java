package com.youhuifuwu.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserPasswordUpdateRequest {

    @NotBlank(message = "oldPassword cannot be blank")
    private String oldPassword;

    @NotBlank(message = "newPassword cannot be blank")
    private String newPassword;
}
