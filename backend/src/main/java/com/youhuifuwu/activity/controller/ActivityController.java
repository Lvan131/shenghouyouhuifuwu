package com.youhuifuwu.activity.controller;

import com.youhuifuwu.activity.dto.ActivitySaveRequest;
import com.youhuifuwu.activity.dto.JoinActivityRequest;
import com.youhuifuwu.activity.service.ActivityService;
import com.youhuifuwu.common.constant.RoleConstants;
import com.youhuifuwu.common.model.ApiResponse;
import com.youhuifuwu.security.AuthContextService;
import com.youhuifuwu.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ActivityController {

    private final ActivityService activityService;
    private final AuthContextService authContextService;

    public ActivityController(ActivityService activityService, AuthContextService authContextService) {
        this.activityService = activityService;
        this.authContextService = authContextService;
    }

    @GetMapping("/activities")
    public ApiResponse<List<Map<String, Object>>> activities(@RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String merchantType) {
        return ApiResponse.success(activityService.listApprovedActivities(keyword, merchantType));
    }

    @GetMapping("/activities/{activityId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long activityId) {
        return ApiResponse.success(activityService.getActivityDetail(activityId));
    }

    @PostMapping("/activities/{activityId}/join")
    public ApiResponse<Void> join(HttpServletRequest request,
                                  @PathVariable Long activityId,
                                  @Valid @RequestBody JoinActivityRequest body) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.USER);
        activityService.joinActivity(currentUser.getAccountId(), activityId, body);
        return ApiResponse.success("joined", null);
    }

    @DeleteMapping("/activities/{activityId}/join")
    public ApiResponse<Void> cancelJoin(HttpServletRequest request, @PathVariable Long activityId) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.USER);
        activityService.cancelJoinActivity(currentUser.getAccountId(), activityId);
        return ApiResponse.success("canceled", null);
    }

    @PostMapping("/activities/{activityId}/cancel-join")
    public ApiResponse<Void> cancelJoinByPost(HttpServletRequest request, @PathVariable Long activityId) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.USER);
        activityService.cancelJoinActivity(currentUser.getAccountId(), activityId);
        return ApiResponse.success("canceled", null);
    }

    @GetMapping("/merchant/activities")
    public ApiResponse<List<Map<String, Object>>> merchantActivities(HttpServletRequest request) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.MERCHANT);
        return ApiResponse.success(activityService.listMerchantActivities(currentUser.getAccountId()));
    }

    @PostMapping("/merchant/activities")
    public ApiResponse<Map<String, Object>> create(HttpServletRequest request,
                                                   @Valid @RequestBody ActivitySaveRequest body) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.MERCHANT);
        return ApiResponse.success(activityService.createActivity(currentUser.getAccountId(), body));
    }

    @PutMapping("/merchant/activities/{activityId}")
    public ApiResponse<Map<String, Object>> update(HttpServletRequest request,
                                                   @PathVariable Long activityId,
                                                   @Valid @RequestBody ActivitySaveRequest body) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.MERCHANT);
        return ApiResponse.success(activityService.updateActivity(currentUser.getAccountId(), activityId, body));
    }

    @PostMapping("/merchant/activities/{activityId}/offline")
    public ApiResponse<Void> offline(HttpServletRequest request, @PathVariable Long activityId) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.MERCHANT);
        activityService.offlineActivity(currentUser.getAccountId(), activityId);
        return ApiResponse.success("offlined", null);
    }

    @GetMapping("/merchant/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(HttpServletRequest request) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.MERCHANT);
        return ApiResponse.success(activityService.getMerchantDashboard(currentUser.getAccountId()));
    }
}
