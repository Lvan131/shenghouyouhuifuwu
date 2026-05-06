package com.youhuifuwu.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youhuifuwu.activity.entity.Activity;
import com.youhuifuwu.activity.entity.ActivityJoinRecord;
import com.youhuifuwu.activity.mapper.ActivityJoinRecordMapper;
import com.youhuifuwu.activity.mapper.ActivityMapper;
import com.youhuifuwu.auth.entity.SysAccount;
import com.youhuifuwu.auth.mapper.SysAccountMapper;
import com.youhuifuwu.common.exception.BusinessException;
import com.youhuifuwu.common.util.MapUtils;
import com.youhuifuwu.merchant.entity.MerchantProfile;
import com.youhuifuwu.merchant.entity.MerchantRating;
import com.youhuifuwu.merchant.mapper.MerchantProfileMapper;
import com.youhuifuwu.merchant.mapper.MerchantRatingMapper;
import com.youhuifuwu.user.dto.UserOnboardingRequest;
import com.youhuifuwu.user.dto.UserPasswordUpdateRequest;
import com.youhuifuwu.user.dto.UserProfileUpdateRequest;
import com.youhuifuwu.user.entity.UserProfile;
import com.youhuifuwu.user.mapper.UserProfileMapper;
import com.youhuifuwu.user.service.UserService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {

    private final UserProfileMapper userProfileMapper;
    private final SysAccountMapper sysAccountMapper;
    private final ActivityJoinRecordMapper activityJoinRecordMapper;
    private final ActivityMapper activityMapper;
    private final MerchantProfileMapper merchantProfileMapper;
    private final MerchantRatingMapper merchantRatingMapper;

    public UserServiceImpl(UserProfileMapper userProfileMapper,
                           SysAccountMapper sysAccountMapper,
                           ActivityJoinRecordMapper activityJoinRecordMapper,
                           ActivityMapper activityMapper,
                           MerchantProfileMapper merchantProfileMapper,
                           MerchantRatingMapper merchantRatingMapper) {
        this.userProfileMapper = userProfileMapper;
        this.sysAccountMapper = sysAccountMapper;
        this.activityJoinRecordMapper = activityJoinRecordMapper;
        this.activityMapper = activityMapper;
        this.merchantProfileMapper = merchantProfileMapper;
        this.merchantRatingMapper = merchantRatingMapper;
    }

    @Override
    public Map<String, Object> getCurrentUserProfile(Long accountId) {
        SysAccount account = getAccount(accountId);
        UserProfile profile = getOrCreateProfile(accountId);
        return buildProfileResponse(account, profile);
    }

    @Override
    @Transactional
    public Map<String, Object> updateCurrentUserProfile(Long accountId, UserProfileUpdateRequest request) {
        SysAccount account = getAccount(accountId);
        UserProfile profile = getOrCreateProfile(accountId);
        ensureUserNoUnique(request.getUserNo(), profile.getId());
        profile.setRealName(trimToNull(request.getRealName()));
        profile.setUserNo(trimToNull(request.getUserNo()));
        profile.setUserType(trimToNull(request.getUserType()));
        profile.setPhone(trimToNull(request.getPhone()));
        profile.setAvatarUrl(trimToNull(request.getAvatarUrl()));
        userProfileMapper.updateById(profile);
        return buildProfileResponse(account, profile);
    }

    @Override
    @Transactional
    public Map<String, Object> completeCurrentUserOnboarding(Long accountId, UserOnboardingRequest request) {
        SysAccount account = getAccount(accountId);
        UserProfile profile = getOrCreateProfile(accountId);
        ensureUserNoUnique(request.getUserNo(), profile.getId());
        profile.setRealName(trimToNull(request.getRealName()));
        profile.setUserNo(trimToNull(request.getUserNo()));
        profile.setUserType(trimToNull(request.getUserType()));
        profile.setPhone(trimToNull(request.getPhone()));
        profile.setAvatarUrl(trimToNull(request.getAvatarUrl()));
        userProfileMapper.updateById(profile);
        account.setPasswordHash(request.getPassword().trim());
        sysAccountMapper.updateById(account);
        return buildProfileResponse(account, profile);
    }

    @Override
    @Transactional
    public void updateCurrentUserPassword(Long accountId, UserPasswordUpdateRequest request) {
        SysAccount account = getAccount(accountId);
        if (!StringUtils.hasText(account.getPasswordHash())) {
            throw new BusinessException(400, "Please complete password setup first");
        }
        if (!account.getPasswordHash().equals(request.getOldPassword())) {
            throw new BusinessException(400, "Current password is incorrect");
        }
        account.setPasswordHash(request.getNewPassword());
        sysAccountMapper.updateById(account);
    }

    @Override
    public List<Map<String, Object>> getMyActivities(Long accountId) {
        UserProfile profile = getOrCreateProfile(accountId);
        List<ActivityJoinRecord> joinRecords = activityJoinRecordMapper.selectList(
                new LambdaQueryWrapper<ActivityJoinRecord>()
                        .eq(ActivityJoinRecord::getUserId, profile.getId())
                        .orderByDesc(ActivityJoinRecord::getJoinTime)
        );
        if (joinRecords.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> activityIds = joinRecords.stream().map(ActivityJoinRecord::getActivityId).collect(Collectors.toSet());
        Map<Long, Activity> activityMap = activityMapper.selectBatchIds(activityIds)
                .stream()
                .collect(Collectors.toMap(Activity::getId, item -> item));
        Set<Long> merchantIds = activityMap.values().stream()
                .map(Activity::getMerchantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, MerchantProfile> merchantMap = merchantIds.isEmpty()
                ? Collections.emptyMap()
                : merchantProfileMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(MerchantProfile::getId, item -> item));
        Map<Long, MerchantRating> ratingMap = merchantIds.isEmpty()
                ? Collections.emptyMap()
                : merchantRatingMapper.selectList(
                        new LambdaQueryWrapper<MerchantRating>()
                                .eq(MerchantRating::getUserId, profile.getId())
                                .in(MerchantRating::getMerchantId, merchantIds)
                ).stream().collect(Collectors.toMap(MerchantRating::getMerchantId, item -> item));

        return joinRecords.stream()
                .map(record -> {
                    Activity activity = activityMap.get(record.getActivityId());
                    if (activity == null) {
                        return null;
                    }
                    MerchantProfile merchant = merchantMap.get(activity.getMerchantId());
                    MerchantRating rating = ratingMap.get(activity.getMerchantId());
                    boolean completed = isCompleted(record, activity);
                    return MapUtils.of(
                            "recordId", record.getId(),
                            "activityId", activity.getId(),
                            "title", activity.getTitle(),
                            "activityType", activity.getActivityType(),
                            "merchantId", activity.getMerchantId(),
                            "merchantName", merchant == null ? "" : merchant.getMerchantName(),
                            "status", record.getStatus(),
                            "joinTime", record.getJoinTime(),
                            "participationDate", record.getParticipationDate(),
                            "completed", completed,
                            "canRate", completed && !"CANCELED".equals(record.getStatus()),
                            "rated", rating != null,
                            "ratingScore", rating == null ? null : rating.getScore()
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean isCompleted(ActivityJoinRecord record, Activity activity) {
        if ("USED".equalsIgnoreCase(record.getStatus())) {
            return true;
        }
        if ("CANCELED".equalsIgnoreCase(record.getStatus())) {
            return false;
        }
        if (record.getParticipationDate() != null && record.getParticipationDate().isBefore(java.time.LocalDate.now())) {
            return true;
        }
        return activity.getEndTime() != null && activity.getEndTime().isBefore(java.time.LocalDateTime.now());
    }

    private Map<String, Object> buildProfileResponse(SysAccount account, UserProfile profile) {
        boolean passwordConfigured = StringUtils.hasText(account.getPasswordHash());
        boolean profileCompleted = isProfileCompleted(profile);
        return MapUtils.of(
                "accountId", account.getId(),
                "role", account.getRole(),
                "displayName", StringUtils.hasText(profile.getRealName()) ? profile.getRealName() : "微信用户",
                "profileCompleted", profileCompleted,
                "passwordConfigured", passwordConfigured,
                "needProfileCompletion", !profileCompleted || !passwordConfigured,
                "profile", profile
        );
    }

    private boolean isProfileCompleted(UserProfile profile) {
        return profile != null
                && StringUtils.hasText(profile.getRealName())
                && StringUtils.hasText(profile.getUserNo())
                && StringUtils.hasText(profile.getUserType())
                && StringUtils.hasText(profile.getPhone());
    }

    private SysAccount getAccount(Long accountId) {
        SysAccount account = sysAccountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(404, "Account not found");
        }
        return account;
    }

    private void ensureUserNoUnique(String userNo, Long currentProfileId) {
        UserProfile existing = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserNo, trimToNull(userNo))
        );
        if (existing != null && !Objects.equals(existing.getId(), currentProfileId)) {
            throw new BusinessException(400, "User number already exists");
        }
    }

    private UserProfile getOrCreateProfile(Long accountId) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getAccountId, accountId)
        );
        if (profile == null) {
            profile = new UserProfile();
            profile.setAccountId(accountId);
            profile.setUserType("STUDENT");
            userProfileMapper.insert(profile);
        }
        return profile;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
