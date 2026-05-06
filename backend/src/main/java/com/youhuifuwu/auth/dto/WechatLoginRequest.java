package com.youhuifuwu.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WechatLoginRequest {

    @NotBlank(message = "mockOpenid cannot be blank")
    private String mockOpenid;

    private String nickname;
}

