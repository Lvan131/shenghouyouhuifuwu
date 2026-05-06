package com.youhuifuwu.admin.service;

import com.youhuifuwu.admin.dto.AdminMerchantSaveRequest;
import com.youhuifuwu.admin.dto.AdminUserSaveRequest;
import java.util.List;
import java.util.Map;

public interface AdminService {

    List<Map<String, Object>> listUsers();

    Map<String, Object> createUser(AdminUserSaveRequest request);

    Map<String, Object> updateUser(Long accountId, AdminUserSaveRequest request);

    void deleteUser(Long accountId);

    List<Map<String, Object>> listMerchants();

    Map<String, Object> createMerchant(AdminMerchantSaveRequest request);

    Map<String, Object> updateMerchant(Long merchantId, AdminMerchantSaveRequest request);

    void deleteMerchant(Long merchantId);
}
