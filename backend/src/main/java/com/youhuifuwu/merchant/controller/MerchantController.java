package com.youhuifuwu.merchant.controller;

import com.youhuifuwu.common.constant.RoleConstants;
import com.youhuifuwu.common.model.ApiResponse;
import com.youhuifuwu.merchant.dto.MerchantProfileUpdateRequest;
import com.youhuifuwu.merchant.dto.MerchantRatingSaveRequest;
import com.youhuifuwu.merchant.service.MerchantService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;
    private final AuthContextService authContextService;

    public MerchantController(MerchantService merchantService, AuthContextService authContextService) {
        this.merchantService = merchantService;
        this.authContextService = authContextService;
    }

    @GetMapping("/nearby")
    public ApiResponse<List<Map<String, Object>>> nearby(@RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String merchantType) {
        return ApiResponse.success(merchantService.listNearbyMerchants(keyword, merchantType));
    }

    @GetMapping("/{merchantId}/activities")
    public ApiResponse<List<Map<String, Object>>> merchantActivities(@PathVariable Long merchantId) {
        return ApiResponse.success(merchantService.listMerchantActivities(merchantId));
    }

    @GetMapping("/{merchantId}/rating/me")
    public ApiResponse<Map<String, Object>> myRating(HttpServletRequest request,
                                                     @PathVariable Long merchantId,
                                                     @RequestParam(required = false) Long activityId) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.USER);
        return ApiResponse.success(
                merchantService.getMyRating(currentUser.getAccountId(), merchantId, activityId)
        );
    }

    @PostMapping("/{merchantId}/ratings")
    public ApiResponse<Map<String, Object>> saveRating(HttpServletRequest request,
                                                       @PathVariable Long merchantId,
                                                       @Valid @RequestBody MerchantRatingSaveRequest body) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.USER);
        return ApiResponse.success(merchantService.saveRating(currentUser.getAccountId(), merchantId, body));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(HttpServletRequest request) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.MERCHANT);
        return ApiResponse.success(merchantService.getCurrentMerchantInfo(currentUser.getAccountId()));
    }

    @PutMapping("/me")
    public ApiResponse<Map<String, Object>> updateMe(HttpServletRequest request,
                                                     @Valid @RequestBody MerchantProfileUpdateRequest body) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.MERCHANT);
        return ApiResponse.success(merchantService.updateCurrentMerchantInfo(currentUser.getAccountId(), body));
    }

    @GetMapping("/me/ratings")
    public ApiResponse<List<Map<String, Object>>> myMerchantRatings(HttpServletRequest request) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.MERCHANT);
        return ApiResponse.success(merchantService.listCurrentMerchantRatings(currentUser.getAccountId()));
    }

    @GetMapping("/subscriptions")
    public ApiResponse<List<Map<String, Object>>> subscriptions(HttpServletRequest request) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.USER);
        return ApiResponse.success(merchantService.listMySubscriptions(currentUser.getAccountId()));
    }

    @PostMapping("/{merchantId}/subscribe")
    public ApiResponse<Void> subscribe(HttpServletRequest request, @PathVariable Long merchantId) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.USER);
        merchantService.subscribe(currentUser.getAccountId(), merchantId);
        return ApiResponse.success("subscribed", null);
    }

    @DeleteMapping("/{merchantId}/subscribe")
    public ApiResponse<Void> unsubscribe(HttpServletRequest request, @PathVariable Long merchantId) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.USER);
        merchantService.unsubscribe(currentUser.getAccountId(), merchantId);
        return ApiResponse.success("unsubscribed", null);
    }

    @PostMapping("/{merchantId}/unsubscribe")
    public ApiResponse<Void> unsubscribeByPost(HttpServletRequest request, @PathVariable Long merchantId) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.USER);
        merchantService.unsubscribe(currentUser.getAccountId(), merchantId);
        return ApiResponse.success("unsubscribed", null);
    }

    @GetMapping("/recommendations")
    public ApiResponse<List<Map<String, Object>>> recommendations(HttpServletRequest request) {
        CurrentUser currentUser = authContextService.requireRole(request, RoleConstants.USER);
        return ApiResponse.success(merchantService.recommendActivities(currentUser.getAccountId()));
    }
}
