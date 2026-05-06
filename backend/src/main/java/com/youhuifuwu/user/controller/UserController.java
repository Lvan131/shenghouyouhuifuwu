package com.youhuifuwu.user.controller;

import com.youhuifuwu.common.constant.RoleConstants;
import com.youhuifuwu.common.model.ApiResponse;
import com.youhuifuwu.security.AuthContextService;
import com.youhuifuwu.security.CurrentUser;
import com.youhuifuwu.user.dto.UserPasswordUpdateRequest;
import com.youhuifuwu.user.dto.UserProfileUpdateRequest;
import com.youhuifuwu.user.dto.UserOnboardingRequest;
import com.youhuifuwu.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;
    private final AuthContextService authContextService;

    public UserController(UserService userService, AuthContextService authContextService) {
        this.userService = userService;
        this.authContextService = authContextService;
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> getProfile(HttpServletRequest request) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.USER);
        return ApiResponse.success(userService.getCurrentUserProfile(currentUser.getAccountId()));
    }

    @PutMapping("/profile")
    public ApiResponse<Map<String, Object>> updateProfile(HttpServletRequest request,
                                                          @Valid @RequestBody UserProfileUpdateRequest body) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.USER);
        return ApiResponse.success(userService.updateCurrentUserProfile(currentUser.getAccountId(), body));
    }

    @PutMapping("/onboarding")
    public ApiResponse<Map<String, Object>> completeOnboarding(HttpServletRequest request,
                                                               @Valid @RequestBody UserOnboardingRequest body) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.USER);
        return ApiResponse.success(userService.completeCurrentUserOnboarding(currentUser.getAccountId(), body));
    }

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(HttpServletRequest request,
                                            @Valid @RequestBody UserPasswordUpdateRequest body) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.USER);
        userService.updateCurrentUserPassword(currentUser.getAccountId(), body);
        return ApiResponse.success("updated", null);
    }

    @GetMapping("/activities")
    public ApiResponse<List<Map<String, Object>>> myActivities(HttpServletRequest request) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.USER);
        return ApiResponse.success(userService.getMyActivities(currentUser.getAccountId()));
    }
}
