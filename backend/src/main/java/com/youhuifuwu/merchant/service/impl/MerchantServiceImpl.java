package com.youhuifuwu.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youhuifuwu.activity.entity.Activity;
import com.youhuifuwu.activity.entity.ActivityJoinRecord;
import com.youhuifuwu.activity.mapper.ActivityJoinRecordMapper;
import com.youhuifuwu.activity.mapper.ActivityMapper;
import com.youhuifuwu.auth.entity.SysAccount;
import com.youhuifuwu.auth.mapper.SysAccountMapper;
import com.youhuifuwu.common.constant.RoleConstants;
import com.youhuifuwu.common.exception.BusinessException;
import com.youhuifuwu.common.util.MapUtils;
import com.youhuifuwu.merchant.dto.MerchantProfileUpdateRequest;
import com.youhuifuwu.merchant.dto.MerchantRatingSaveRequest;
import com.youhuifuwu.merchant.entity.MerchantProfile;
import com.youhuifuwu.merchant.entity.MerchantRating;
import com.youhuifuwu.merchant.entity.MerchantSubscription;
import com.youhuifuwu.merchant.mapper.MerchantProfileMapper;
import com.youhuifuwu.merchant.mapper.MerchantRatingMapper;
import com.youhuifuwu.merchant.mapper.MerchantSubscriptionMapper;
import com.youhuifuwu.merchant.service.MerchantService;
import com.youhuifuwu.user.entity.UserProfile;
import com.youhuifuwu.user.mapper.UserProfileMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MerchantServiceImpl implements MerchantService {

    private static final BigDecimal MAX_DISTANCE = new BigDecimal("999999");

    private final MerchantProfileMapper merchantProfileMapper;
    private final MerchantSubscriptionMapper merchantSubscriptionMapper;
    private final MerchantRatingMapper merchantRatingMapper;
    private final UserProfileMapper userProfileMapper;
    private final ActivityMapper activityMapper;
    private final ActivityJoinRecordMapper activityJoinRecordMapper;
    private final SysAccountMapper sysAccountMapper;

    public MerchantServiceImpl(MerchantProfileMapper merchantProfileMapper,
                               MerchantSubscriptionMapper merchantSubscriptionMapper,
                               MerchantRatingMapper merchantRatingMapper,
                               UserProfileMapper userProfileMapper,
                               ActivityMapper activityMapper,
                               ActivityJoinRecordMapper activityJoinRecordMapper,
                               SysAccountMapper sysAccountMapper) {
        this.merchantProfileMapper = merchantProfileMapper;
        this.merchantSubscriptionMapper = merchantSubscriptionMapper;
        this.merchantRatingMapper = merchantRatingMapper;
        this.userProfileMapper = userProfileMapper;
        this.activityMapper = activityMapper;
        this.activityJoinRecordMapper = activityJoinRecordMapper;
        this.sysAccountMapper = sysAccountMapper;
    }

    @Override
    public List<Map<String, Object>> listNearbyMerchants(String keyword, String merchantType) {
        LambdaQueryWrapper<MerchantProfile> wrapper = new LambdaQueryWrapper<MerchantProfile>()
                .eq(MerchantProfile::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(MerchantProfile::getMerchantName, keyword);
        }
        if (StringUtils.hasText(merchantType)) {
            wrapper.eq(MerchantProfile::getMerchantType, merchantType);
        }
        List<MerchantProfile> merchants = merchantProfileMapper.selectList(wrapper);
        if (merchants.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> merchantIds = merchants.stream().map(MerchantProfile::getId).collect(Collectors.toSet());
        Map<Long, Long> activityCountMap = activityMapper.selectList(
                        new LambdaQueryWrapper<Activity>()
                                .in(Activity::getMerchantId, merchantIds)
                                .eq(Activity::getStatus, "APPROVED")
                ).stream()
                .collect(Collectors.groupingBy(Activity::getMerchantId, Collectors.counting()));
        Map<Long, RatingSummary> ratingSummaryMap = buildRatingSummaryMap(merchantIds);

        return merchants.stream()
                .sorted(Comparator
                        .comparing((MerchantProfile merchant) -> distanceOrMax(merchant.getDistanceKm()))
                        .thenComparing(MerchantProfile::getMerchantName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(merchant -> {
                    RatingSummary ratingSummary = ratingSummaryMap.getOrDefault(merchant.getId(), RatingSummary.empty());
                    return MapUtils.of(
                            "merchantId", merchant.getId(),
                            "merchantName", merchant.getMerchantName(),
                            "merchantType", merchant.getMerchantType(),
                            "address", merchant.getAddress(),
                            "distanceKm", merchant.getDistanceKm(),
                            "contactPhone", merchant.getContactPhone() == null ? "" : merchant.getContactPhone(),
                            "activityCount", activityCountMap.getOrDefault(merchant.getId(), 0L),
                            "avgScore", ratingSummary.avgScore(),
                            "ratingCount", ratingSummary.ratingCount()
                    );
                })
                .toList();
    }

    @Override
    public List<Map<String, Object>> listMerchantActivities(Long merchantId) {
        MerchantProfile merchant = merchantProfileMapper.selectById(merchantId);
        if (merchant == null || merchant.getStatus() == null || merchant.getStatus() != 1) {
            throw new BusinessException(404, "Merchant not found");
        }
        RatingSummary ratingSummary = buildRatingSummaryMap(Set.of(merchantId))
                .getOrDefault(merchantId, RatingSummary.empty());

        return activityMapper.selectList(
                        new LambdaQueryWrapper<Activity>()
                                .eq(Activity::getMerchantId, merchantId)
                                .eq(Activity::getStatus, "APPROVED")
                                .orderByDesc(Activity::getCreatedAt)
                ).stream()
                .map(activity -> MapUtils.of(
                        "activityId", activity.getId(),
                        "title", activity.getTitle(),
                        "activityType", activity.getActivityType(),
                        "content", activity.getContent(),
                        "merchantId", merchant.getId(),
                        "merchantName", merchant.getMerchantName(),
                        "merchantType", merchant.getMerchantType(),
                        "distanceKm", merchant.getDistanceKm(),
                        "address", merchant.getAddress(),
                        "joinedCount", activity.getJoinedCount(),
                        "quota", activity.getQuota(),
                        "dailyQuota", activity.getDailyQuota(),
                        "startTime", activity.getStartTime(),
                        "endTime", activity.getEndTime(),
                        "avgScore", ratingSummary.avgScore(),
                        "ratingCount", ratingSummary.ratingCount()
                ))
                .toList();
    }

    @Override
    public Map<String, Object> getCurrentMerchantInfo(Long accountId) {
        SysAccount account = getMerchantAccount(accountId);
        MerchantProfile merchant = getMerchantProfileByAccountId(accountId);
        return buildCurrentMerchantInfo(account, merchant);
    }

    @Override
    @Transactional
    public Map<String, Object> updateCurrentMerchantInfo(Long accountId, MerchantProfileUpdateRequest request) {
        SysAccount account = getMerchantAccount(accountId);
        MerchantProfile merchant = getMerchantProfileByAccountId(accountId);
        ensureUsernameUnique(request.getUsername(), account.getId());

        account.setUsername(trimToNull(request.getUsername()));
        if (StringUtils.hasText(request.getPassword())) {
            account.setPasswordHash(request.getPassword().trim());
        }
        sysAccountMapper.updateById(account);

        merchant.setMerchantName(trimToNull(request.getMerchantName()));
        merchant.setMerchantType(trimToNull(request.getMerchantType()));
        merchant.setContactName(trimToNull(request.getContactName()));
        merchant.setContactPhone(trimToNull(request.getContactPhone()));
        merchant.setAddress(trimToNull(request.getAddress()));
        merchant.setDistanceKm(request.getDistanceKm());
        merchant.setLongitude(request.getLongitude());
        merchant.setLatitude(request.getLatitude());
        merchant.setDescription(trimToNull(request.getDescription()));
        merchantProfileMapper.updateById(merchant);

        return buildCurrentMerchantInfo(account, merchant);
    }

    @Override
    public List<Map<String, Object>> listCurrentMerchantRatings(Long accountId) {
        MerchantProfile merchant = getMerchantProfileByAccountId(accountId);
        List<MerchantRating> ratings = merchantRatingMapper.selectList(
                new LambdaQueryWrapper<MerchantRating>()
                        .eq(MerchantRating::getMerchantId, merchant.getId())
                        .orderByDesc(MerchantRating::getUpdatedAt)
                        .orderByDesc(MerchantRating::getCreatedAt)
        );
        if (ratings.isEmpty()) {
            return Collections.emptyList();
        }

        RatingRelationBundle bundle = loadRatingRelationBundle(ratings);
        return ratings.stream()
                .map(rating -> buildMerchantRatingRow(rating, bundle))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<Map<String, Object>> listMySubscriptions(Long accountId) {
        UserProfile userProfile = getUserProfile(accountId);
        List<MerchantSubscription> subscriptions = merchantSubscriptionMapper.selectList(
                new LambdaQueryWrapper<MerchantSubscription>()
                        .eq(MerchantSubscription::getUserId, userProfile.getId())
                        .orderByDesc(MerchantSubscription::getCreatedAt)
        );
        if (subscriptions.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> merchantIds = subscriptions.stream().map(MerchantSubscription::getMerchantId).collect(Collectors.toSet());
        Map<Long, MerchantProfile> merchantMap = merchantProfileMapper.selectBatchIds(merchantIds)
                .stream()
                .collect(Collectors.toMap(MerchantProfile::getId, Function.identity()));
        Map<Long, RatingSummary> ratingSummaryMap = buildRatingSummaryMap(merchantIds);

        return subscriptions.stream()
                .map(item -> {
                    MerchantProfile merchant = merchantMap.get(item.getMerchantId());
                    if (merchant == null) {
                        return null;
                    }
                    RatingSummary ratingSummary = ratingSummaryMap.getOrDefault(merchant.getId(), RatingSummary.empty());
                    return MapUtils.of(
                            "subscriptionId", item.getId(),
                            "merchantId", merchant.getId(),
                            "merchantName", merchant.getMerchantName(),
                            "merchantType", merchant.getMerchantType(),
                            "address", merchant.getAddress(),
                            "distanceKm", merchant.getDistanceKm(),
                            "avgScore", ratingSummary.avgScore(),
                            "ratingCount", ratingSummary.ratingCount(),
                            "createdAt", item.getCreatedAt()
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void subscribe(Long accountId, Long merchantId) {
        UserProfile userProfile = getUserProfile(accountId);
        MerchantProfile merchant = merchantProfileMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(404, "Merchant not found");
        }
        Long count = merchantSubscriptionMapper.selectCount(
                new LambdaQueryWrapper<MerchantSubscription>()
                        .eq(MerchantSubscription::getUserId, userProfile.getId())
                        .eq(MerchantSubscription::getMerchantId, merchantId)
        );
        if (count > 0) {
            return;
        }
        MerchantSubscription subscription = new MerchantSubscription();
        subscription.setUserId(userProfile.getId());
        subscription.setMerchantId(merchantId);
        merchantSubscriptionMapper.insert(subscription);
    }

    @Override
    public void unsubscribe(Long accountId, Long merchantId) {
        UserProfile userProfile = getUserProfile(accountId);
        merchantSubscriptionMapper.delete(
                new LambdaQueryWrapper<MerchantSubscription>()
                        .eq(MerchantSubscription::getUserId, userProfile.getId())
                        .eq(MerchantSubscription::getMerchantId, merchantId)
        );
    }

    @Override
    public List<Map<String, Object>> recommendActivities(Long accountId) {
        UserProfile userProfile = getUserProfile(accountId);
        List<MerchantSubscription> subscriptions = merchantSubscriptionMapper.selectList(
                new LambdaQueryWrapper<MerchantSubscription>()
                        .eq(MerchantSubscription::getUserId, userProfile.getId())
        );
        if (subscriptions.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> subscribedMerchantIds = subscriptions.stream()
                .map(MerchantSubscription::getMerchantId)
                .collect(Collectors.toSet());
        Set<String> merchantTypes = merchantProfileMapper.selectBatchIds(subscribedMerchantIds).stream()
                .map(MerchantProfile::getMerchantType)
                .collect(Collectors.toSet());
        if (merchantTypes.isEmpty()) {
            return Collections.emptyList();
        }
        List<MerchantProfile> merchants = merchantProfileMapper.selectList(
                new LambdaQueryWrapper<MerchantProfile>()
                        .in(MerchantProfile::getMerchantType, merchantTypes)
                        .eq(MerchantProfile::getStatus, 1)
        );
        Map<Long, MerchantProfile> merchantMap = merchants.stream()
                .collect(Collectors.toMap(MerchantProfile::getId, Function.identity()));
        Set<Long> merchantIds = merchantMap.keySet();
        if (merchantIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, RatingSummary> ratingSummaryMap = buildRatingSummaryMap(merchantIds);

        return activityMapper.selectList(
                        new LambdaQueryWrapper<Activity>()
                                .in(Activity::getMerchantId, merchantIds)
                                .eq(Activity::getStatus, "APPROVED")
                ).stream()
                .sorted(Comparator
                        .comparing((Activity activity) -> distanceOrMax(merchantMap.get(activity.getMerchantId()).getDistanceKm()))
                        .thenComparing(Activity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(activity -> {
                    MerchantProfile merchant = merchantMap.get(activity.getMerchantId());
                    if (merchant == null) {
                        return null;
                    }
                    RatingSummary ratingSummary = ratingSummaryMap.getOrDefault(merchant.getId(), RatingSummary.empty());
                    return MapUtils.of(
                            "activityId", activity.getId(),
                            "title", activity.getTitle(),
                            "activityType", activity.getActivityType(),
                            "content", activity.getContent(),
                            "merchantId", merchant.getId(),
                            "merchantName", merchant.getMerchantName(),
                            "merchantType", merchant.getMerchantType(),
                            "distanceKm", merchant.getDistanceKm(),
                            "address", merchant.getAddress(),
                            "joinedCount", activity.getJoinedCount(),
                            "quota", activity.getQuota(),
                            "startTime", activity.getStartTime(),
                            "endTime", activity.getEndTime(),
                            "avgScore", ratingSummary.avgScore(),
                            "ratingCount", ratingSummary.ratingCount()
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public Map<String, Object> getMyRating(Long accountId, Long merchantId, Long activityId) {
        UserProfile userProfile = getUserProfile(accountId);
        MerchantProfile merchant = getMerchantProfile(merchantId);
        MerchantRating existingRating = merchantRatingMapper.selectOne(
                new LambdaQueryWrapper<MerchantRating>()
                        .eq(MerchantRating::getMerchantId, merchantId)
                        .eq(MerchantRating::getUserId, userProfile.getId())
        );
        RatingEligibility eligibility = resolveRatingEligibility(userProfile.getId(), merchantId, activityId);
        RatingSummary ratingSummary = buildRatingSummaryMap(Set.of(merchantId))
                .getOrDefault(merchantId, RatingSummary.empty());

        return MapUtils.of(
                "merchantId", merchant.getId(),
                "merchantName", merchant.getMerchantName(),
                "activityId", eligibility.activityId(),
                "activityTitle", eligibility.activityTitle(),
                "canRate", eligibility.canRate(),
                "reason", eligibility.reason(),
                "rated", existingRating != null,
                "score", existingRating == null ? null : existingRating.getScore(),
                "content", existingRating == null ? "" : defaultString(existingRating.getContent()),
                "avgScore", ratingSummary.avgScore(),
                "ratingCount", ratingSummary.ratingCount()
        );
    }

    @Override
    @Transactional
    public Map<String, Object> saveRating(Long accountId, Long merchantId, MerchantRatingSaveRequest request) {
        UserProfile userProfile = getUserProfile(accountId);
        MerchantProfile merchant = getMerchantProfile(merchantId);
        RatingEligibility eligibility = resolveRatingEligibility(userProfile.getId(), merchantId, request.getActivityId());
        if (!eligibility.canRate()) {
            throw new BusinessException(400, eligibility.reason());
        }

        MerchantRating rating = merchantRatingMapper.selectOne(
                new LambdaQueryWrapper<MerchantRating>()
                        .eq(MerchantRating::getMerchantId, merchantId)
                        .eq(MerchantRating::getUserId, userProfile.getId())
        );
        if (rating == null) {
            rating = new MerchantRating();
            rating.setMerchantId(merchantId);
            rating.setUserId(userProfile.getId());
            rating.setActivityId(eligibility.activityId());
            rating.setScore(request.getScore());
            rating.setContent(trimToNull(request.getContent()));
            merchantRatingMapper.insert(rating);
        } else {
            rating.setActivityId(eligibility.activityId());
            rating.setScore(request.getScore());
            rating.setContent(trimToNull(request.getContent()));
            merchantRatingMapper.updateById(rating);
        }

        RatingSummary ratingSummary = buildRatingSummaryMap(Set.of(merchantId))
                .getOrDefault(merchantId, RatingSummary.empty());
        return MapUtils.of(
                "merchantId", merchant.getId(),
                "merchantName", merchant.getMerchantName(),
                "score", rating.getScore(),
                "content", defaultString(rating.getContent()),
                "avgScore", ratingSummary.avgScore(),
                "ratingCount", ratingSummary.ratingCount()
        );
    }

    private Map<String, Object> buildCurrentMerchantInfo(SysAccount account, MerchantProfile merchant) {
        RatingSummary ratingSummary = buildRatingSummaryMap(Set.of(merchant.getId()))
                .getOrDefault(merchant.getId(), RatingSummary.empty());
        return MapUtils.of(
                "accountId", account.getId(),
                "username", account.getUsername(),
                "status", account.getStatus(),
                "merchantId", merchant.getId(),
                "merchantName", merchant.getMerchantName(),
                "merchantType", merchant.getMerchantType(),
                "contactName", defaultString(merchant.getContactName()),
                "contactPhone", defaultString(merchant.getContactPhone()),
                "address", merchant.getAddress(),
                "distanceKm", merchant.getDistanceKm(),
                "longitude", merchant.getLongitude(),
                "latitude", merchant.getLatitude(),
                "description", defaultString(merchant.getDescription()),
                "avgScore", ratingSummary.avgScore(),
                "ratingCount", ratingSummary.ratingCount(),
                "createdAt", merchant.getCreatedAt()
        );
    }

    private Map<String, Object> buildMerchantRatingRow(MerchantRating rating, RatingRelationBundle bundle) {
        UserProfile user = bundle.userMap().get(rating.getUserId());
        Activity activity = bundle.activityMap().get(rating.getActivityId());
        if (user == null) {
            return null;
        }
        return MapUtils.of(
                "ratingId", rating.getId(),
                "userId", user.getId(),
                "userName", StringUtils.hasText(user.getRealName()) ? user.getRealName() : "微信用户",
                "userNo", defaultString(user.getUserNo()),
                "userType", defaultString(user.getUserType()),
                "phone", defaultString(user.getPhone()),
                "activityId", activity == null ? null : activity.getId(),
                "activityTitle", activity == null ? "" : defaultString(activity.getTitle()),
                "activityType", activity == null ? "" : defaultString(activity.getActivityType()),
                "participationDate", bundle.participationDateMap().get(rating.getId()),
                "score", rating.getScore(),
                "content", defaultString(rating.getContent()),
                "createdAt", rating.getCreatedAt(),
                "updatedAt", rating.getUpdatedAt()
        );
    }

    private RatingRelationBundle loadRatingRelationBundle(List<MerchantRating> ratings) {
        Set<Long> userIds = ratings.stream()
                .map(MerchantRating::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> activityIds = ratings.stream()
                .map(MerchantRating::getActivityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, UserProfile> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userProfileMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getId, Function.identity()));
        Map<Long, Activity> activityMap = activityIds.isEmpty()
                ? Collections.emptyMap()
                : activityMapper.selectBatchIds(activityIds).stream()
                .collect(Collectors.toMap(Activity::getId, Function.identity()));
        Map<Long, LocalDate> participationDateMap = buildParticipationDateMap(ratings);
        return new RatingRelationBundle(userMap, activityMap, participationDateMap);
    }

    private Map<Long, LocalDate> buildParticipationDateMap(List<MerchantRating> ratings) {
        Set<Long> activityIds = ratings.stream()
                .map(MerchantRating::getActivityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> userIds = ratings.stream()
                .map(MerchantRating::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (activityIds.isEmpty() || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, LocalDate> recordMap = activityJoinRecordMapper.selectList(
                        new LambdaQueryWrapper<ActivityJoinRecord>()
                                .in(ActivityJoinRecord::getActivityId, activityIds)
                                .in(ActivityJoinRecord::getUserId, userIds)
                ).stream()
                .collect(Collectors.toMap(
                        item -> item.getActivityId() + "_" + item.getUserId(),
                        ActivityJoinRecord::getParticipationDate,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Map<Long, LocalDate> participationDateMap = new LinkedHashMap<>();
        ratings.forEach(rating -> participationDateMap.put(
                rating.getId(),
                recordMap.get(rating.getActivityId() + "_" + rating.getUserId())
        ));
        return participationDateMap;
    }

    private RatingEligibility resolveRatingEligibility(Long userId, Long merchantId, Long activityId) {
        MerchantProfile merchant = getMerchantProfile(merchantId);
        LambdaQueryWrapper<ActivityJoinRecord> joinWrapper = new LambdaQueryWrapper<ActivityJoinRecord>()
                .eq(ActivityJoinRecord::getUserId, userId)
                .ne(ActivityJoinRecord::getStatus, "CANCELED")
                .orderByDesc(ActivityJoinRecord::getJoinTime);
        List<ActivityJoinRecord> joinRecords = activityJoinRecordMapper.selectList(joinWrapper);
        if (joinRecords.isEmpty()) {
            return RatingEligibility.notAllowed("You have not joined this merchant's activity yet");
        }

        Set<Long> joinedActivityIds = joinRecords.stream()
                .map(ActivityJoinRecord::getActivityId)
                .collect(Collectors.toSet());
        Map<Long, Activity> activityMap = activityMapper.selectBatchIds(joinedActivityIds).stream()
                .collect(Collectors.toMap(Activity::getId, Function.identity()));

        List<ActivityJoinRecord> merchantJoinRecords = joinRecords.stream()
                .filter(item -> {
                    Activity activity = activityMap.get(item.getActivityId());
                    if (activity == null || !Objects.equals(activity.getMerchantId(), merchant.getId())) {
                        return false;
                    }
                    return activityId == null || Objects.equals(activity.getId(), activityId);
                })
                .toList();
        if (merchantJoinRecords.isEmpty()) {
            return RatingEligibility.notAllowed("No eligible activity record found");
        }

        for (ActivityJoinRecord joinRecord : merchantJoinRecords) {
            Activity activity = activityMap.get(joinRecord.getActivityId());
            if (activity == null) {
                continue;
            }
            if (isCompleted(joinRecord, activity)) {
                return RatingEligibility.allowed(activity.getId(), activity.getTitle());
            }
        }

        return RatingEligibility.notAllowed("The activity is not completed yet");
    }

    private boolean isCompleted(ActivityJoinRecord joinRecord, Activity activity) {
        if ("USED".equalsIgnoreCase(joinRecord.getStatus())) {
            return true;
        }
        LocalDate today = LocalDate.now();
        if (joinRecord.getParticipationDate() != null && joinRecord.getParticipationDate().isBefore(today)) {
            return true;
        }
        return activity.getEndTime() != null && activity.getEndTime().isBefore(LocalDateTime.now());
    }

    private Map<Long, RatingSummary> buildRatingSummaryMap(Set<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<MerchantRating>> groupedRatings = merchantRatingMapper.selectList(
                        new LambdaQueryWrapper<MerchantRating>().in(MerchantRating::getMerchantId, merchantIds)
                ).stream()
                .collect(Collectors.groupingBy(MerchantRating::getMerchantId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, RatingSummary> summaryMap = new LinkedHashMap<>();
        groupedRatings.forEach((merchantId, ratings) -> summaryMap.put(merchantId, buildRatingSummary(ratings)));
        return summaryMap;
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

    private UserProfile getUserProfile(Long accountId) {
        UserProfile userProfile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getAccountId, accountId)
        );
        if (userProfile == null) {
            throw new BusinessException(404, "User profile not found");
        }
        return userProfile;
    }

    private SysAccount getMerchantAccount(Long accountId) {
        SysAccount account = sysAccountMapper.selectById(accountId);
        if (account == null || !RoleConstants.MERCHANT.equals(account.getRole())) {
            throw new BusinessException(404, "Merchant account not found");
        }
        return account;
    }

    private MerchantProfile getMerchantProfileByAccountId(Long accountId) {
        MerchantProfile merchant = merchantProfileMapper.selectOne(
                new LambdaQueryWrapper<MerchantProfile>().eq(MerchantProfile::getAccountId, accountId)
        );
        if (merchant == null) {
            throw new BusinessException(404, "Merchant profile not found");
        }
        return merchant;
    }

    private MerchantProfile getMerchantProfile(Long merchantId) {
        MerchantProfile merchant = merchantProfileMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(404, "Merchant not found");
        }
        return merchant;
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private BigDecimal distanceOrMax(BigDecimal distanceKm) {
        return distanceKm == null ? MAX_DISTANCE : distanceKm;
    }

    private record RatingSummary(BigDecimal avgScore, int ratingCount) {
        private static RatingSummary empty() {
            return new RatingSummary(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP), 0);
        }
    }

    private record RatingRelationBundle(Map<Long, UserProfile> userMap,
                                        Map<Long, Activity> activityMap,
                                        Map<Long, LocalDate> participationDateMap) {
    }

    private record RatingEligibility(boolean canRate, Long activityId, String activityTitle, String reason) {
        private static RatingEligibility allowed(Long activityId, String activityTitle) {
            return new RatingEligibility(true, activityId, activityTitle, "");
        }

        private static RatingEligibility notAllowed(String reason) {
            return new RatingEligibility(false, null, "", reason);
        }
    }
}
