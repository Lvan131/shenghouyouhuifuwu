package com.youhuifuwu.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youhuifuwu.activity.dto.ActivitySaveRequest;
import com.youhuifuwu.activity.dto.JoinActivityRequest;
import com.youhuifuwu.activity.entity.Activity;
import com.youhuifuwu.activity.entity.ActivityAuditRecord;
import com.youhuifuwu.activity.entity.ActivityJoinRecord;
import com.youhuifuwu.activity.mapper.ActivityAuditRecordMapper;
import com.youhuifuwu.activity.mapper.ActivityJoinRecordMapper;
import com.youhuifuwu.activity.mapper.ActivityMapper;
import com.youhuifuwu.activity.service.ActivityService;
import com.youhuifuwu.admin.dto.ActivityAuditRequest;
import com.youhuifuwu.common.exception.BusinessException;
import com.youhuifuwu.common.util.MapUtils;
import com.youhuifuwu.merchant.entity.MerchantProfile;
import com.youhuifuwu.merchant.entity.MerchantRating;
import com.youhuifuwu.merchant.mapper.MerchantProfileMapper;
import com.youhuifuwu.merchant.mapper.MerchantRatingMapper;
import com.youhuifuwu.user.entity.UserProfile;
import com.youhuifuwu.user.mapper.UserProfileMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ActivityServiceImpl implements ActivityService {

    private static final BigDecimal MAX_DISTANCE = new BigDecimal("999999");

    private final ActivityMapper activityMapper;
    private final ActivityJoinRecordMapper activityJoinRecordMapper;
    private final ActivityAuditRecordMapper activityAuditRecordMapper;
    private final MerchantProfileMapper merchantProfileMapper;
    private final MerchantRatingMapper merchantRatingMapper;
    private final UserProfileMapper userProfileMapper;

    public ActivityServiceImpl(ActivityMapper activityMapper,
                               ActivityJoinRecordMapper activityJoinRecordMapper,
                               ActivityAuditRecordMapper activityAuditRecordMapper,
                               MerchantProfileMapper merchantProfileMapper,
                               MerchantRatingMapper merchantRatingMapper,
                               UserProfileMapper userProfileMapper) {
        this.activityMapper = activityMapper;
        this.activityJoinRecordMapper = activityJoinRecordMapper;
        this.activityAuditRecordMapper = activityAuditRecordMapper;
        this.merchantProfileMapper = merchantProfileMapper;
        this.merchantRatingMapper = merchantRatingMapper;
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    public List<Map<String, Object>> listApprovedActivities(String keyword, String merchantType) {
        List<MerchantProfile> merchants = merchantProfileMapper.selectList(
                new LambdaQueryWrapper<MerchantProfile>().eq(MerchantProfile::getStatus, 1)
        );
        if (merchants.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, MerchantProfile> merchantMap = merchants.stream()
                .collect(Collectors.toMap(MerchantProfile::getId, item -> item));
        Set<Long> merchantIds = merchantMap.keySet();
        Map<Long, RatingSummary> ratingSummaryMap = buildRatingSummaryMap(merchantIds);

        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .in(Activity::getMerchantId, merchantIds)
                .eq(Activity::getStatus, "APPROVED")
                .orderByDesc(Activity::getCreatedAt);

        return activityMapper.selectList(wrapper).stream()
                .filter(activity -> {
                    MerchantProfile merchant = merchantMap.get(activity.getMerchantId());
                    if (merchant == null) {
                        return false;
                    }
                    boolean matchType = !StringUtils.hasText(merchantType)
                            || merchantType.equals(merchant.getMerchantType());
                    boolean matchKeyword = !StringUtils.hasText(keyword)
                            || activity.getTitle().contains(keyword)
                            || merchant.getMerchantName().contains(keyword);
                    return matchType && matchKeyword;
                })
                .sorted(Comparator
                        .comparing((Activity activity) -> distanceOrMax(merchantMap.get(activity.getMerchantId()).getDistanceKm()))
                        .thenComparing(Activity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(activity -> {
                    MerchantProfile merchant = merchantMap.get(activity.getMerchantId());
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
                            "avgScore", ratingSummary.avgScore(),
                            "ratingCount", ratingSummary.ratingCount(),
                            "joinedCount", activity.getJoinedCount(),
                            "quota", activity.getQuota(),
                            "dailyQuota", activity.getDailyQuota(),
                            "startTime", activity.getStartTime(),
                            "endTime", activity.getEndTime()
                    );
                })
                .toList();
    }

    @Override
    public Map<String, Object> getActivityDetail(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(404, "Activity not found");
        }
        MerchantProfile merchant = merchantProfileMapper.selectById(activity.getMerchantId());
        RatingSummary ratingSummary = buildRatingSummaryMap(Set.of(activity.getMerchantId()))
                .getOrDefault(activity.getMerchantId(), RatingSummary.empty());
        return MapUtils.of(
                "activity", activity,
                "merchant", MapUtils.of(
                        "id", merchant.getId(),
                        "merchantName", merchant.getMerchantName(),
                        "merchantType", merchant.getMerchantType(),
                        "address", merchant.getAddress(),
                        "distanceKm", merchant.getDistanceKm(),
                        "contactPhone", merchant.getContactPhone(),
                        "avgScore", ratingSummary.avgScore(),
                        "ratingCount", ratingSummary.ratingCount()
                ),
                "scheduleOptions", buildScheduleOptions(activity)
        );
    }

    @Override
    @Transactional
    public void joinActivity(Long accountId, Long activityId, JoinActivityRequest request) {
        UserProfile userProfile = getUserProfile(accountId);
        Activity activity = getActivity(activityId);
        LocalDate participationDate = request.getParticipationDate();

        ensureJoinable(activity, participationDate);

        ActivityJoinRecord existingRecord = activityJoinRecordMapper.selectOne(
                new LambdaQueryWrapper<ActivityJoinRecord>()
                        .eq(ActivityJoinRecord::getActivityId, activityId)
                        .eq(ActivityJoinRecord::getUserId, userProfile.getId())
        );
        if (existingRecord != null && "JOINED".equals(existingRecord.getStatus())) {
            throw new BusinessException(400, "You have already joined this activity");
        }

        long dailyJoinedCount = countJoinedByDate(activityId, participationDate);
        if (activity.getDailyQuota() != null && dailyJoinedCount >= activity.getDailyQuota()) {
            throw new BusinessException(400, "Selected date quota is full");
        }

        if (existingRecord != null) {
            existingRecord.setStatus("JOINED");
            existingRecord.setParticipationDate(participationDate);
            existingRecord.setJoinTime(LocalDateTime.now());
            existingRecord.setRemark(null);
            activityJoinRecordMapper.updateById(existingRecord);
        } else {
            ActivityJoinRecord joinRecord = new ActivityJoinRecord();
            joinRecord.setActivityId(activityId);
            joinRecord.setUserId(userProfile.getId());
            joinRecord.setParticipationDate(participationDate);
            joinRecord.setStatus("JOINED");
            joinRecord.setJoinTime(LocalDateTime.now());
            activityJoinRecordMapper.insert(joinRecord);
        }
        activity.setJoinedCount((activity.getJoinedCount() == null ? 0 : activity.getJoinedCount()) + 1);
        activityMapper.updateById(activity);
    }

    @Override
    @Transactional
    public void cancelJoinActivity(Long accountId, Long activityId) {
        UserProfile userProfile = getUserProfile(accountId);
        Activity activity = getActivity(activityId);
        ActivityJoinRecord joinRecord = activityJoinRecordMapper.selectOne(
                new LambdaQueryWrapper<ActivityJoinRecord>()
                        .eq(ActivityJoinRecord::getActivityId, activityId)
                        .eq(ActivityJoinRecord::getUserId, userProfile.getId())
        );
        if (joinRecord == null || !"JOINED".equals(joinRecord.getStatus())) {
            throw new BusinessException(400, "You have not joined this activity");
        }
        joinRecord.setStatus("CANCELED");
        joinRecord.setRemark("User canceled");
        activityJoinRecordMapper.updateById(joinRecord);
        int joinedCount = activity.getJoinedCount() == null ? 0 : activity.getJoinedCount();
        activity.setJoinedCount(Math.max(0, joinedCount - 1));
        activityMapper.updateById(activity);
    }

    @Override
    public List<Map<String, Object>> listMerchantActivities(Long accountId) {
        MerchantProfile merchant = getMerchantByAccountId(accountId);
        return activityMapper.selectList(
                        new LambdaQueryWrapper<Activity>()
                                .eq(Activity::getMerchantId, merchant.getId())
                                .orderByDesc(Activity::getCreatedAt)
                ).stream()
                .map(activity -> MapUtils.of(
                        "activityId", activity.getId(),
                        "title", activity.getTitle(),
                        "activityType", activity.getActivityType(),
                        "content", activity.getContent(),
                        "status", activity.getStatus(),
                        "joinedCount", activity.getJoinedCount(),
                        "quota", activity.getQuota(),
                        "dailyQuota", activity.getDailyQuota(),
                        "startTime", activity.getStartTime(),
                        "endTime", activity.getEndTime()
                ))
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> createActivity(Long accountId, ActivitySaveRequest request) {
        MerchantProfile merchant = getMerchantByAccountId(accountId);
        Activity activity = new Activity();
        fillActivity(activity, request);
        activity.setMerchantId(merchant.getId());
        activity.setStatus("PENDING");
        activity.setJoinedCount(0);
        activityMapper.insert(activity);
        return MapUtils.of("activity", activity);
    }

    @Override
    @Transactional
    public Map<String, Object> updateActivity(Long accountId, Long activityId, ActivitySaveRequest request) {
        MerchantProfile merchant = getMerchantByAccountId(accountId);
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || !merchant.getId().equals(activity.getMerchantId())) {
            throw new BusinessException(404, "Activity not found");
        }
        fillActivity(activity, request);
        activity.setStatus("PENDING");
        activity.setAuditRemark(null);
        activity.setAuditedAt(null);
        activity.setAuditedBy(null);
        activityMapper.updateById(activity);
        return MapUtils.of("activity", activity);
    }

    @Override
    public void offlineActivity(Long accountId, Long activityId) {
        MerchantProfile merchant = getMerchantByAccountId(accountId);
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || !merchant.getId().equals(activity.getMerchantId())) {
            throw new BusinessException(404, "Activity not found");
        }
        activity.setStatus("OFFLINE");
        activityMapper.updateById(activity);
    }

    @Override
    public Map<String, Object> getMerchantDashboard(Long accountId) {
        MerchantProfile merchant = getMerchantByAccountId(accountId);
        List<Activity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<Activity>().eq(Activity::getMerchantId, merchant.getId())
        );
        int activityCount = activities.size();
        int joinedCount = activities.stream().map(Activity::getJoinedCount).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        long pendingCount = activities.stream().filter(item -> "PENDING".equals(item.getStatus())).count();
        return MapUtils.of(
                "merchantId", merchant.getId(),
                "merchantName", merchant.getMerchantName(),
                "activityCount", activityCount,
                "joinedCount", joinedCount,
                "pendingCount", pendingCount
        );
    }

    @Override
    public List<Map<String, Object>> listPendingActivities() {
        List<Activity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<Activity>()
                        .eq(Activity::getStatus, "PENDING")
                        .orderByDesc(Activity::getCreatedAt)
        );
        if (activities.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> merchantIds = activities.stream().map(Activity::getMerchantId).collect(Collectors.toSet());
        Map<Long, MerchantProfile> merchantMap = merchantProfileMapper.selectBatchIds(merchantIds)
                .stream()
                .collect(Collectors.toMap(MerchantProfile::getId, item -> item));
        return activities.stream()
                .map(activity -> {
                    MerchantProfile merchant = merchantMap.get(activity.getMerchantId());
                    return MapUtils.of(
                            "activityId", activity.getId(),
                            "title", activity.getTitle(),
                            "activityType", activity.getActivityType(),
                            "merchantName", merchant == null ? "" : merchant.getMerchantName(),
                            "createdAt", activity.getCreatedAt(),
                            "status", activity.getStatus()
                    );
                })
                .toList();
    }

    @Override
    public List<Map<String, Object>> listAuditActivities() {
        List<Activity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<Activity>()
                        .ne(Activity::getStatus, "DRAFT")
                        .orderByDesc(Activity::getCreatedAt)
        );
        if (activities.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> merchantIds = activities.stream().map(Activity::getMerchantId).collect(Collectors.toSet());
        Map<Long, MerchantProfile> merchantMap = merchantProfileMapper.selectBatchIds(merchantIds)
                .stream()
                .collect(Collectors.toMap(MerchantProfile::getId, item -> item));

        return activities.stream()
                .map(activity -> {
                    MerchantProfile merchant = merchantMap.get(activity.getMerchantId());
                    boolean pending = "PENDING".equals(activity.getStatus());
                    return MapUtils.of(
                            "activityId", activity.getId(),
                            "title", activity.getTitle(),
                            "activityType", activity.getActivityType(),
                            "merchantName", merchant == null ? "" : merchant.getMerchantName(),
                            "createdAt", activity.getCreatedAt(),
                            "status", activity.getStatus(),
                            "auditStatus", pending ? "PENDING" : "REVIEWED",
                            "auditRemark", activity.getAuditRemark() == null ? "" : activity.getAuditRemark(),
                            "auditedAt", activity.getAuditedAt()
                    );
                })
                .toList();
    }

    @Override
    @Transactional
    public void auditActivity(Long adminAccountId, Long activityId, ActivityAuditRequest request) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(404, "Activity not found");
        }
        boolean approved = "APPROVED".equalsIgnoreCase(request.getAuditResult());
        activity.setStatus(approved ? "APPROVED" : "REJECTED");
        activity.setAuditRemark(request.getAuditRemark());
        activity.setAuditedBy(adminAccountId);
        activity.setAuditedAt(LocalDateTime.now());
        activityMapper.updateById(activity);

        ActivityAuditRecord auditRecord = new ActivityAuditRecord();
        auditRecord.setActivityId(activityId);
        auditRecord.setAuditorId(adminAccountId);
        auditRecord.setAuditResult(approved ? "APPROVED" : "REJECTED");
        auditRecord.setAuditRemark(request.getAuditRemark());
        auditRecord.setCreatedAt(LocalDateTime.now());
        activityAuditRecordMapper.insert(auditRecord);
    }

    private void ensureJoinable(Activity activity, LocalDate participationDate) {
        if (!"APPROVED".equals(activity.getStatus())) {
            throw new BusinessException(400, "Activity is not available");
        }
        if (participationDate == null) {
            throw new BusinessException(400, "Participation date is required");
        }
        LocalDate startDate = activity.getStartTime().toLocalDate();
        LocalDate endDate = activity.getEndTime().toLocalDate();
        if (participationDate.isBefore(startDate) || participationDate.isAfter(endDate)) {
            throw new BusinessException(400, "Participation date is out of range");
        }
        if (activity.getQuota() != null && activity.getJoinedCount() != null && activity.getJoinedCount() >= activity.getQuota()) {
            throw new BusinessException(400, "Activity quota is full");
        }
    }

    private List<Map<String, Object>> buildScheduleOptions(Activity activity) {
        LocalDate startDate = activity.getStartTime().toLocalDate();
        LocalDate endDate = activity.getEndTime().toLocalDate();
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        Map<LocalDate, Long> joinedCountMap = activityJoinRecordMapper.selectList(
                        new LambdaQueryWrapper<ActivityJoinRecord>()
                                .eq(ActivityJoinRecord::getActivityId, activity.getId())
                                .eq(ActivityJoinRecord::getStatus, "JOINED")
                ).stream()
                .filter(item -> item.getParticipationDate() != null)
                .collect(Collectors.groupingBy(ActivityJoinRecord::getParticipationDate, Collectors.counting()));

        List<Map<String, Object>> options = new ArrayList<>();
        for (long i = 0; i <= days; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            int joinedCount = joinedCountMap.getOrDefault(currentDate, 0L).intValue();
            int dailyQuota = activity.getDailyQuota() == null ? 0 : activity.getDailyQuota();
            int remainingQuota = Math.max(0, dailyQuota - joinedCount);
            options.add(MapUtils.of(
                    "date", currentDate,
                    "joinedCount", joinedCount,
                    "dailyQuota", activity.getDailyQuota(),
                    "remainingQuota", remainingQuota,
                    "full", activity.getDailyQuota() != null && joinedCount >= activity.getDailyQuota()
            ));
        }
        return options;
    }

    private long countJoinedByDate(Long activityId, LocalDate participationDate) {
        return activityJoinRecordMapper.selectCount(
                new LambdaQueryWrapper<ActivityJoinRecord>()
                        .eq(ActivityJoinRecord::getActivityId, activityId)
                        .eq(ActivityJoinRecord::getParticipationDate, participationDate)
                        .eq(ActivityJoinRecord::getStatus, "JOINED")
        );
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

    private Activity getActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(404, "Activity not found");
        }
        return activity;
    }

    private MerchantProfile getMerchantByAccountId(Long accountId) {
        MerchantProfile merchant = merchantProfileMapper.selectOne(
                new LambdaQueryWrapper<MerchantProfile>().eq(MerchantProfile::getAccountId, accountId)
        );
        if (merchant == null) {
            throw new BusinessException(404, "Merchant profile not found");
        }
        return merchant;
    }

    private void fillActivity(Activity activity, ActivitySaveRequest request) {
        validateActivityRequest(request);
        activity.setTitle(request.getTitle());
        activity.setActivityType(request.getActivityType());
        activity.setCoverImage(request.getCoverImage());
        activity.setContent(request.getContent());
        activity.setQuota(request.getQuota());
        activity.setDailyQuota(request.getDailyQuota());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
    }

    private void validateActivityRequest(ActivitySaveRequest request) {
        if (request.getStartTime() != null && request.getEndTime() != null
                && request.getEndTime().isBefore(request.getStartTime())) {
            throw new BusinessException(400, "End time must be after start time");
        }
        if (request.getQuota() != null && request.getDailyQuota() != null && request.getQuota() < request.getDailyQuota()) {
            throw new BusinessException(400, "Total quota cannot be less than daily quota");
        }
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
