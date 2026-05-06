package com.youhuifuwu.announcement.service;

import com.youhuifuwu.announcement.dto.AnnouncementCreateRequest;
import java.util.List;
import java.util.Map;

public interface AnnouncementService {

    List<Map<String, Object>> listPublishedAnnouncements();

    List<Map<String, Object>> listAdminAnnouncements();

    Map<String, Object> createAnnouncement(Long adminAccountId, AnnouncementCreateRequest request);

    Map<String, Object> updateAnnouncement(Long announcementId, Long adminAccountId, AnnouncementCreateRequest request);
}
