package com.youhuifuwu.activity.service;

import com.youhuifuwu.activity.dto.ActivitySaveRequest;
import com.youhuifuwu.activity.dto.JoinActivityRequest;
import com.youhuifuwu.admin.dto.ActivityAuditRequest;
import java.util.List;
import java.util.Map;

public interface ActivityService {

    List<Map<String, Object>> listApprovedActivities(String keyword, String merchantType);

    Map<String, Object> getActivityDetail(Long activityId);

    void joinActivity(Long accountId, Long activityId, JoinActivityRequest request);

    void cancelJoinActivity(Long accountId, Long activityId);

    List<Map<String, Object>> listMerchantActivities(Long accountId);

    Map<String, Object> createActivity(Long accountId, ActivitySaveRequest request);

    Map<String, Object> updateActivity(Long accountId, Long activityId, ActivitySaveRequest request);

    void offlineActivity(Long accountId, Long activityId);

    Map<String, Object> getMerchantDashboard(Long accountId);

    List<Map<String, Object>> listPendingActivities();

    List<Map<String, Object>> listAuditActivities();

    void auditActivity(Long adminAccountId, Long activityId, ActivityAuditRequest request);
}
