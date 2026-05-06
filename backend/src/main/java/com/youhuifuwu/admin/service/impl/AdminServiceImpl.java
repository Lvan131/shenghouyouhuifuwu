package com.youhuifuwu.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youhuifuwu.activity.entity.Activity;
import com.youhuifuwu.activity.entity.ActivityAuditRecord;
import com.youhuifuwu.activity.entity.ActivityJoinRecord;
import com.youhuifuwu.activity.mapper.ActivityAuditRecordMapper;
import com.youhuifuwu.activity.mapper.ActivityJoinRecordMapper;
import com.youhuifuwu.activity.mapper.ActivityMapper;
import com.youhuifuwu.admin.dto.AdminMerchantSaveRequest;
import com.youhuifuwu.admin.dto.AdminUserSaveRequest;
import com.youhuifuwu.admin.service.AdminService;
import com.youhuifuwu.auth.entity.SysAccount;
import com.youhuifuwu.auth.mapper.SysAccountMapper;
import com.youhuifuwu.common.constant.LoginTypeConstants;
import com.youhuifuwu.common.constant.RoleConstants;
import com.youhuifuwu.common.exception.BusinessException;
import com.youhuifuwu.common.util.MapUtils;
import com.youhuifuwu.merchant.entity.MerchantProfile;
import com.youhuifuwu.merchant.entity.MerchantRating;
import com.youhuifuwu.merchant.entity.MerchantSubscription;
import com.youhuifuwu.merchant.mapper.MerchantProfileMapper;
import com.youhuifuwu.merchant.mapper.MerchantRatingMapper;
import com.youhuifuwu.merchant.mapper.MerchantSubscriptionMapper;
import com.youhuifuwu.user.entity.UserProfile;
import com.youhuifuwu.user.mapper.UserProfileMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminServiceImpl implements AdminService {

    private static final BigDecimal MAX_DISTANCE = new BigDecimal("999999");

    private final SysAccountMapper sysAccountMapper;
    private final UserProfileMapper userProfileMapper;
    private final MerchantProfileMapper merchantProfileMapper;
    private final MerchantSubscriptionMapper merchantSubscriptionMapper;
    private final MerchantRatingMapper merchantRatingMapper;
    private final ActivityMapper activityMapper;
    private final ActivityJoinRecordMapper activityJoinRecordMapper;
    private final ActivityAuditRecordMapper activityAuditRecordMapper;

    public AdminServiceImpl(SysAccountMapper sysAccountMapper,
                            UserProfileMapper userProfileMapper,
                            MerchantProfileMapper merchantProfileMapper,
                            MerchantSubscriptionMapper merchantSubscriptionMapper,
                            MerchantRatingMapper merchantRatingMapper,
                            ActivityMapper activityMapper,
                            ActivityJoinRecordMapper activityJoinRecordMapper,
                            ActivityAuditRecordMapper activityAuditRecordMapper) {
        this.sysAccountMapper = sysAccountMapper;
        this.userProfileMapper = userProfileMapper;
        this.merchantProfileMapper = merchantProfileMapper;
        this.merchantSubscriptionMapper = merchantSubscriptionMapper;
        this.merchantRatingMapper = merchantRatingMapper;
        this.activityMapper = activityMapper;
        this.activityJoinRecordMapper = activityJoinRecordMapper;
        this.activityAuditRecordMapper = activityAuditRecordMapper;
    }

    @Override
    public List<Map<String, Object>> listUsers() {
        List<SysAccount> accounts = sysAccountMapper.selectList(
                new LambdaQueryWrapper<SysAccount>()
                        .eq(SysAccount::getRole, RoleConstants.USER)
                        .orderByDesc(SysAccount::getCreatedAt)
        );
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, UserProfile> profileMap = userProfileMapper.selectList(
                        new LambdaQueryWrapper<UserProfile>()
                                .in(UserProfile::getAccountId, accounts.stream().map(SysAccount::getId).toList())
                ).stream()
                .collect(Collectors.toMap(UserProfile::getAccountId, Function.identity()));

        return accounts.stream()
                .map(account -> buildUserRow(account, profileMap.get(account.getId())))
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> createUser(AdminUserSaveRequest request) {
        ensureUserNoUnique(request.getUserNo(), null);
        validateUserCreateRequest(request);
        SysAccount account = new SysAccount();
        account.setRole(RoleConstants.USER);
        account.setLoginType(LoginTypeConstants.PASSWORD);
        account.setWxOpenid(generateUserOpenid());
        account.setPasswordHash(request.getPassword().trim());
        account.setStatus(defaultStatus(request.getStatus()));
        sysAccountMapper.insert(account);

        UserProfile profile = new UserProfile();
        fillUserProfile(profile, request);
        profile.setAccountId(account.getId());
        userProfileMapper.insert(profile);
        return buildUserRow(account, profile);
    }

    @Override
    @Transactional
    public Map<String, Object> updateUser(Long accountId, AdminUserSaveRequest request) {
        SysAccount account = getAccount(accountId, RoleConstants.USER);
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getAccountId, accountId)
        );
        ensureUserNoUnique(request.getUserNo(), profile == null ? null : profile.getId());
        if (profile == null) {
            profile = new UserProfile();
            profile.setAccountId(accountId);
            fillUserProfile(profile, request);
            userProfileMapper.insert(profile);
        } else {
            fillUserProfile(profile, request);
            userProfileMapper.updateById(profile);
        }
        if (StringUtils.hasText(request.getPassword())) {
            account.setPasswordHash(request.getPassword().trim());
        }
        account.setStatus(defaultStatus(request.getStatus()));
        sysAccountMapper.updateById(account);
        return buildUserRow(account, profile);
    }

    @Override
    @Transactional
    public void deleteUser(Long accountId) {
        getAccount(accountId, RoleConstants.USER);
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getAccountId, accountId)
        );
        if (profile != null) {
            activityJoinRecordMapper.delete(
                    new LambdaQueryWrapper<ActivityJoinRecord>().eq(ActivityJoinRecord::getUserId, profile.getId())
            );
            merchantSubscriptionMapper.delete(
                    new LambdaQueryWrapper<MerchantSubscription>().eq(MerchantSubscription::getUserId, profile.getId())
            );
            merchantRatingMapper.delete(
                    new LambdaQueryWrapper<MerchantRating>().eq(MerchantRating::getUserId, profile.getId())
            );
            userProfileMapper.deleteById(profile.getId());
        }
        sysAccountMapper.deleteById(accountId);
    }

    @Override
    public List<Map<String, Object>> listMerchants() {
        List<MerchantProfile> merchants = merchantProfileMapper.selectList(new LambdaQueryWrapper<MerchantProfile>());
        if (merchants.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, SysAccount> accountMap = sysAccountMapper.selectBatchIds(
                        merchants.stream().map(MerchantProfile::getAccountId).toList()
                ).stream()
                .collect(Collectors.toMap(SysAccount::getId, Function.identity()));
        Map<Long, RatingSummary> ratingSummaryMap = buildRatingSummaryMap(
                merchants.stream().map(MerchantProfile::getId).collect(Collectors.toSet())
        );
        return merchants.stream()
                .sorted(Comparator
                        .comparing((MerchantProfile merchant) -> distanceOrMax(merchant.getDistanceKm()))
                        .thenComparing(MerchantProfile::getMerchantName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(merchant -> buildMerchantRow(
                        accountMap.get(merchant.getAccountId()),
                        merchant,
                        ratingSummaryMap.getOrDefault(merchant.getId(), RatingSummary.empty())
                ))
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> createMerchant(AdminMerchantSaveRequest request) {
        validateMerchantCreateRequest(request);
        ensureUsernameUnique(request.getUsername(), null);

        SysAccount account = new SysAccount();
        account.setUsername(trimToNull(request.getUsername()));
        account.setPasswordHash(request.getPassword().trim());
        account.setRole(RoleConstants.MERCHANT);
        account.setLoginType(LoginTypeConstants.PASSWORD);
        account.setStatus(defaultStatus(request.getStatus()));
        sysAccountMapper.insert(account);

        MerchantProfile merchant = new MerchantProfile();
        fillMerchantProfile(merchant, request);
        merchant.setAccountId(account.getId());
        merchantProfileMapper.insert(merchant);
        return buildMerchantRow(account, merchant, RatingSummary.empty());
    }

    @Override
    @Transactional
    public Map<String, Object> updateMerchant(Long merchantId, AdminMerchantSaveRequest request) {
        MerchantProfile merchant = getMerchantProfile(merchantId);
        SysAccount account = getAccount(merchant.getAccountId(), RoleConstants.MERCHANT);
        ensureUsernameUnique(request.getUsername(), account.getId());
        account.setUsername(trimToNull(request.getUsername()));
        if (StringUtils.hasText(request.getPassword())) {
            account.setPasswordHash(request.getPassword().trim());
        }
        account.setStatus(defaultStatus(request.getStatus()));
        sysAccountMapper.updateById(account);

        fillMerchantProfile(merchant, request);
        merchantProfileMapper.updateById(merchant);
        RatingSummary ratingSummary = buildRatingSummaryMap(Set.of(merchantId))
                .getOrDefault(merchantId, RatingSummary.empty());
        return buildMerchantRow(account, merchant, ratingSummary);
    }

    @Override
    @Transactional
    public void deleteMerchant(Long merchantId) {
        MerchantProfile merchant = getMerchantProfile(merchantId);
        getAccount(merchant.getAccountId(), RoleConstants.MERCHANT);

        List<Activity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<Activity>().eq(Activity::getMerchantId, merchantId)
        );
        if (!activities.isEmpty()) {
            Set<Long> activityIds = activities.stream().map(Activity::getId).collect(Collectors.toSet());
            activityJoinRecordMapper.delete(
                    new LambdaQueryWrapper<ActivityJoinRecord>().in(ActivityJoinRecord::getActivityId, activityIds)
            );
            activityAuditRecordMapper.delete(
                    new LambdaQueryWrapper<ActivityAuditRecord>().in(ActivityAuditRecord::getActivityId, activityIds)
            );
            activityMapper.delete(new LambdaQueryWrapper<Activity>().eq(Activity::getMerchantId, merchantId));
        }

        merchantSubscriptionMapper.delete(
                new LambdaQueryWrapper<MerchantSubscription>().eq(MerchantSubscription::getMerchantId, merchantId)
        );
        merchantRatingMapper.delete(
                new LambdaQueryWrapper<MerchantRating>().eq(MerchantRating::getMerchantId, merchantId)
        );
        merchantProfileMapper.deleteById(merchantId);
        sysAccountMapper.deleteById(merchant.getAccountId());
    }

    private Map<String, Object> buildUserRow(SysAccount account, UserProfile profile) {
        return MapUtils.of(
                "accountId", account.getId(),
                "displayName", profile != null && StringUtils.hasText(profile.getRealName()) ? profile.getRealName() : "微信用户",
                "userNo", profile == null || profile.getUserNo() == null ? "" : profile.getUserNo(),
                "userType", profile == null || profile.getUserType() == null ? "" : profile.getUserType(),
                "phone", profile == null || profile.getPhone() == null ? "" : profile.getPhone(),
                "avatarUrl", profile == null || profile.getAvatarUrl() == null ? "" : profile.getAvatarUrl(),
                "status", account.getStatus(),
                "createdAt", account.getCreatedAt()
        );
    }

    private Map<String, Object> buildMerchantRow(SysAccount account,
                                                 MerchantProfile merchant,
                                                 RatingSummary ratingSummary) {
        if (account == null) {
            return Collections.emptyMap();
        }
        return MapUtils.of(
                "merchantId", merchant.getId(),
                "accountId", account.getId(),
                "username", account.getUsername(),
                "merchantName", merchant.getMerchantName(),
                "merchantType", merchant.getMerchantType(),
                "contactName", merchant.getContactName() == null ? "" : merchant.getContactName(),
                "contactPhone", merchant.getContactPhone() == null ? "" : merchant.getContactPhone(),
                "address", merchant.getAddress(),
                "distanceKm", merchant.getDistanceKm(),
                "longitude", merchant.getLongitude(),
                "latitude", merchant.getLatitude(),
                "description", merchant.getDescription() == null ? "" : merchant.getDescription(),
                "avgScore", ratingSummary.avgScore(),
                "ratingCount", ratingSummary.ratingCount(),
                "status", account.getStatus(),
                "createdAt", merchant.getCreatedAt()
        );
    }

    private void fillUserProfile(UserProfile profile, AdminUserSaveRequest request) {
        profile.setRealName(trimToNull(request.getRealName()));
        profile.setUserNo(trimToNull(request.getUserNo()));
        profile.setUserType(trimToNull(request.getUserType()));
        profile.setPhone(trimToNull(request.getPhone()));
        profile.setAvatarUrl(trimToNull(request.getAvatarUrl()));
    }

    private void fillMerchantProfile(MerchantProfile merchant, AdminMerchantSaveRequest request) {
        merchant.setMerchantName(trimToNull(request.getMerchantName()));
        merchant.setMerchantType(trimToNull(request.getMerchantType()));
        merchant.setContactName(trimToNull(request.getContactName()));
        merchant.setContactPhone(trimToNull(request.getContactPhone()));
        merchant.setAddress(trimToNull(request.getAddress()));
        merchant.setDistanceKm(request.getDistanceKm());
        merchant.setLongitude(request.getLongitude());
        merchant.setLatitude(request.getLatitude());
        merchant.setDescription(trimToNull(request.getDescription()));
        merchant.setStatus(defaultStatus(request.getStatus()));
    }

    private void validateMerchantCreateRequest(AdminMerchantSaveRequest request) {
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(400, "Password is required");
        }
    }

    private void validateUserCreateRequest(AdminUserSaveRequest request) {
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(400, "Password is required");
        }
    }

    private void ensureUsernameUnique(String username, Long currentAccountId) {
        String normalizedUsername = trimToNull(username);
        if (!StringUtils.hasText(normalizedUsername)) {
            throw new BusinessException(400, "Username is required");
        }
        SysAccount existing = sysAccountMapper.selectOne(
                new LambdaQueryWrapper<SysAccount>().eq(SysAccount::getUsername, normalizedUsername)
        );
        if (existing != null && !Objects.equals(existing.getId(), currentAccountId)) {
            throw new BusinessException(400, "Username already exists");
        }
    }

    private void ensureUserNoUnique(String userNo, Long currentProfileId) {
        String normalizedUserNo = trimToNull(userNo);
        if (!StringUtils.hasText(normalizedUserNo)) {
            throw new BusinessException(400, "User number is required");
        }
        UserProfile existing = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserNo, normalizedUserNo)
        );
        if (existing != null && !Objects.equals(existing.getId(), currentProfileId)) {
            throw new BusinessException(400, "User number already exists");
        }
    }

    private SysAccount getAccount(Long accountId, String role) {
        SysAccount account = sysAccountMapper.selectById(accountId);
        if (account == null || !role.equals(account.getRole())) {
            throw new BusinessException(404, "Account not found");
        }
        return account;
    }

    private MerchantProfile getMerchantProfile(Long merchantId) {
        MerchantProfile merchant = merchantProfileMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(404, "Merchant not found");
        }
        return merchant;
    }

    private String generateUserOpenid() {
        return "manual-user-" + UUID.randomUUID();
    }

    private Integer defaultStatus(Integer status) {
        return status == null ? 1 : status;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal distanceOrMax(BigDecimal distanceKm) {
        return distanceKm == null ? MAX_DISTANCE : distanceKm;
    }

    private Map<Long, RatingSummary> buildRatingSummaryMap(Set<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return merchantRatingMapper.selectList(
                        new LambdaQueryWrapper<MerchantRating>().in(MerchantRating::getMerchantId, merchantIds)
                ).stream()
                .collect(Collectors.groupingBy(MerchantRating::getMerchantId))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> buildRatingSummary(entry.getValue())));
    }

    private RatingSummary buildRatingSummary(List<MerchantRating> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return RatingSummary.empty();
        }
        BigDecimal totalScore = ratings.stream()
                .map(MerchantRating::getScore)
                .filter(Objects::nonNull)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgScore = totalScore.divide(BigDecimal.valueOf(ratings.size()), 1, RoundingMode.HALF_UP);
        return new RatingSummary(avgScore, ratings.size());
    }

    private record RatingSummary(BigDecimal avgScore, int ratingCount) {
        private static RatingSummary empty() {
            return new RatingSummary(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP), 0);
        }
    }
}
