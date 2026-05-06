package com.youhuifuwu.admin.controller;

import com.youhuifuwu.activity.service.ActivityService;
import com.youhuifuwu.admin.dto.ActivityAuditRequest;
import com.youhuifuwu.admin.dto.AdminMerchantSaveRequest;
import com.youhuifuwu.admin.dto.AdminUserSaveRequest;
import com.youhuifuwu.admin.service.AdminService;
import com.youhuifuwu.announcement.dto.AnnouncementCreateRequest;
import com.youhuifuwu.announcement.service.AnnouncementService;
import com.youhuifuwu.common.constant.RoleConstants;
import com.youhuifuwu.common.model.ApiResponse;
import com.youhuifuwu.security.AuthContextService;
import com.youhuifuwu.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final ActivityService activityService;
    private final AnnouncementService announcementService;
    private final AuthContextService authContextService;

    public AdminController(AdminService adminService,
                           ActivityService activityService,
                           AnnouncementService announcementService,
                           AuthContextService authContextService) {
        this.adminService = adminService;
        this.activityService = activityService;
        this.announcementService = announcementService;
        this.authContextService = authContextService;
    }

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> users(HttpServletRequest request) {
        authContextService.requireRole(request, RoleConstants.ADMIN);
        return ApiResponse.success(adminService.listUsers());
    }

    @PostMapping("/users")
    public ApiResponse<Map<String, Object>> createUser(HttpServletRequest request,
                                                       @Valid @RequestBody AdminUserSaveRequest body) {
        authContextService.requireRole(request, RoleConstants.ADMIN);
        return ApiResponse.success(adminService.createUser(body));
    }

    @PutMapping("/users/{accountId}")
    public ApiResponse<Map<String, Object>> updateUser(HttpServletRequest request,
                                                       @PathVariable Long accountId,
                                                       @Valid @RequestBody AdminUserSaveRequest body) {
        authContextService.requireRole(request, RoleConstants.ADMIN);
        return ApiResponse.success(adminService.updateUser(accountId, body));
    }

    @DeleteMapping("/users/{accountId}")
    public ApiResponse<Void> deleteUser(HttpServletRequest request, @PathVariable Long accountId) {
        authContextService.requireRole(request, RoleConstants.ADMIN);
        adminService.deleteUser(accountId);
        return ApiResponse.success("deleted", null);
    }

    @GetMapping("/merchants")
    public ApiResponse<List<Map<String, Object>>> merchants(HttpServletRequest request) {
        authContextService.requireRole(request, RoleConstants.ADMIN);
        return ApiResponse.success(adminService.listMerchants());
    }

    @PostMapping("/merchants")
    public ApiResponse<Map<String, Object>> createMerchant(HttpServletRequest request,
                                                           @Valid @RequestBody AdminMerchantSaveRequest body) {
        authContextService.requireRole(request, RoleConstants.ADMIN);
        return ApiResponse.success(adminService.createMerchant(body));
    }

    @PutMapping("/merchants/{merchantId}")
    public ApiResponse<Map<String, Object>> updateMerchant(HttpServletRequest request,
                                                           @PathVariable Long merchantId,
                                                           @Valid @RequestBody AdminMerchantSaveRequest body) {
        authContextService.requireRole(request, RoleConstants.ADMIN);
        return ApiResponse.success(adminService.updateMerchant(merchantId, body));
    }

    @DeleteMapping("/merchants/{merchantId}")
    public ApiResponse<Void> deleteMerchant(HttpServletRequest request, @PathVariable Long merchantId) {
        authContextService.requireRole(request, RoleConstants.ADMIN);
        adminService.deleteMerchant(merchantId);
        return ApiResponse.success("deleted", null);
    }

    @GetMapping("/activities/pending")
    public ApiResponse<List<Map<String, Object>>> pendingActivities(HttpServletRequest request) {
        authContextService.requireRole(request, RoleConstants.ADMIN);
        return ApiResponse.success(activityService.listPendingActivities());
    }

    @GetMapping("/activities")
    public ApiResponse<List<Map<String, Object>>> auditActivities(HttpServletRequest request) {
        authContextService.requireRole(request, RoleConstants.ADMIN);
        return ApiResponse.success(activityService.listAuditActivities());
    }

    @PostMapping("/activities/{activityId}/audit")
    public ApiResponse<Void> audit(HttpServletRequest request,
                                   @PathVariable Long activityId,
                                   @Valid @RequestBody ActivityAuditRequest body) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.ADMIN);
        activityService.auditActivity(currentUser.getAccountId(), activityId, body);
        return ApiResponse.success("audited", null);
    }

    @GetMapping("/announcements")
    public ApiResponse<List<Map<String, Object>>> announcements(HttpServletRequest request) {
        authContextService.requireRole(request, RoleConstants.ADMIN);
        return ApiResponse.success(announcementService.listAdminAnnouncements());
    }

    @PostMapping("/announcements")
    public ApiResponse<Map<String, Object>> createAnnouncement(HttpServletRequest request,
                                                               @Valid @RequestBody AnnouncementCreateRequest body) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.ADMIN);
        return ApiResponse.success(announcementService.createAnnouncement(currentUser.getAccountId(), body));
    }

    @PutMapping("/announcements/{announcementId}")
    public ApiResponse<Map<String, Object>> updateAnnouncement(HttpServletRequest request,
                                                               @PathVariable Long announcementId,
                                                               @Valid @RequestBody AnnouncementCreateRequest body) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.ADMIN);
        return ApiResponse.success(
                announcementService.updateAnnouncement(announcementId, currentUser.getAccountId(), body)
        );
    }
}
