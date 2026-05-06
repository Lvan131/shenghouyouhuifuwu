package com.youhuifuwu.user.service;

import com.youhuifuwu.user.dto.UserOnboardingRequest;
import com.youhuifuwu.user.dto.UserPasswordUpdateRequest;
import com.youhuifuwu.user.dto.UserProfileUpdateRequest;
import java.util.List;
import java.util.Map;

public interface UserService {

    Map<String, Object> getCurrentUserProfile(Long accountId);

    Map<String, Object> updateCurrentUserProfile(Long accountId, UserProfileUpdateRequest request);

    Map<String, Object> completeCurrentUserOnboarding(Long accountId, UserOnboardingRequest request);

    void updateCurrentUserPassword(Long accountId, UserPasswordUpdateRequest request);

    List<Map<String, Object>> getMyActivities(Long accountId);
}
