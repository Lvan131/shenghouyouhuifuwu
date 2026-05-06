package com.youhuifuwu.merchant.service;

import com.youhuifuwu.merchant.dto.MerchantRatingSaveRequest;
import com.youhuifuwu.merchant.dto.MerchantProfileUpdateRequest;
import java.util.List;
import java.util.Map;

public interface MerchantService {

    List<Map<String, Object>> listNearbyMerchants(String keyword, String merchantType);

    List<Map<String, Object>> listMySubscriptions(Long accountId);

    void subscribe(Long accountId, Long merchantId);

    void unsubscribe(Long accountId, Long merchantId);

    List<Map<String, Object>> recommendActivities(Long accountId);

    List<Map<String, Object>> listMerchantActivities(Long merchantId);

    Map<String, Object> getCurrentMerchantInfo(Long accountId);

    Map<String, Object> updateCurrentMerchantInfo(Long accountId, MerchantProfileUpdateRequest request);

    List<Map<String, Object>> listCurrentMerchantRatings(Long accountId);

    Map<String, Object> getMyRating(Long accountId, Long merchantId, Long activityId);

    Map<String, Object> saveRating(Long accountId, Long merchantId, MerchantRatingSaveRequest request);
}
