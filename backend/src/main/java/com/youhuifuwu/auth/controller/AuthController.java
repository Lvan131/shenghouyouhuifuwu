package com.youhuifuwu.auth.controller;

import com.youhuifuwu.auth.dto.PasswordLoginRequest;
import com.youhuifuwu.auth.dto.UserRegisterRequest;
import com.youhuifuwu.auth.dto.WechatLoginRequest;
import com.youhuifuwu.auth.service.AuthService;
import com.youhuifuwu.auth.vo.LoginResponse;
import com.youhuifuwu.common.model.ApiResponse;
import com.youhuifuwu.security.AuthContextService;
import com.youhuifuwu.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthContextService authContextService;

    public AuthController(AuthService authService, AuthContextService authContextService) {
        this.authService = authService;
        this.authContextService = authContextService;
    }

    @PostMapping("/password-login")
    public ApiResponse<LoginResponse> passwordLogin(@Valid @RequestBody PasswordLoginRequest request) {
        return ApiResponse.success(authService.passwordLogin(request));
    }

    @PostMapping("/wechat-login")
    public ApiResponse<LoginResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        return ApiResponse.success(authService.wechatLogin(request));
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUser> me(HttpServletRequest request) {
        return ApiResponse.success(authContextService.requireCurrentUser(request));
    }
}
