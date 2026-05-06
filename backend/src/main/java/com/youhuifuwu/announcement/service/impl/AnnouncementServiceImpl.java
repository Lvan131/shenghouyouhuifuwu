package com.youhuifuwu.announcement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youhuifuwu.announcement.dto.AnnouncementCreateRequest;
import com.youhuifuwu.announcement.entity.Announcement;
import com.youhuifuwu.announcement.mapper.AnnouncementMapper;
import com.youhuifuwu.announcement.service.AnnouncementService;
import com.youhuifuwu.common.exception.BusinessException;
import com.youhuifuwu.common.util.MapUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    public AnnouncementServiceImpl(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    @Override
    public List<Map<String, Object>> listPublishedAnnouncements() {
        return announcementMapper.selectList(
                        new LambdaQueryWrapper<Announcement>()
                                .eq(Announcement::getStatus, 1)
                                .orderByDesc(Announcement::getPublishedAt)
                ).stream()
                .map(item -> MapUtils.of(
                        "announcementId", item.getId(),
                        "title", item.getTitle(),
                        "content", item.getContent(),
                        "publishedAt", item.getPublishedAt()
                ))
                .toList();
    }

    @Override
    public List<Map<String, Object>> listAdminAnnouncements() {
        return announcementMapper.selectList(
                        new LambdaQueryWrapper<Announcement>()
                                .orderByDesc(Announcement::getCreatedAt)
                ).stream()
                .map(item -> MapUtils.of(
                        "announcementId", item.getId(),
                        "title", item.getTitle(),
                        "content", item.getContent(),
                        "status", item.getStatus(),
                        "publishedAt", item.getPublishedAt()
                ))
                .toList();
    }

    @Override
    public Map<String, Object> createAnnouncement(Long adminAccountId, AnnouncementCreateRequest request) {
        Announcement announcement = new Announcement();
        fillAnnouncement(announcement, adminAccountId, request);
        announcementMapper.insert(announcement);
        return buildAdminRow(announcement);
    }

    @Override
    public Map<String, Object> updateAnnouncement(Long announcementId, Long adminAccountId, AnnouncementCreateRequest request) {
        Announcement announcement = announcementMapper.selectById(announcementId);
        if (announcement == null) {
            throw new BusinessException(404, "Announcement not found");
        }
        fillAnnouncement(announcement, adminAccountId, request);
        announcementMapper.updateById(announcement);
        return buildAdminRow(announcement);
    }

    private void fillAnnouncement(Announcement announcement, Long adminAccountId, AnnouncementCreateRequest request) {
        int status = request.getStatus() == null ? 1 : request.getStatus();
        announcement.setTitle(request.getTitle().trim());
        announcement.setContent(request.getContent().trim());
        announcement.setStatus(status);
        announcement.setPublishedBy(adminAccountId);
        if (status == 1 && announcement.getPublishedAt() == null) {
            announcement.setPublishedAt(LocalDateTime.now());
        }
    }

    private Map<String, Object> buildAdminRow(Announcement announcement) {
        return MapUtils.of(
                "announcementId", announcement.getId(),
                "title", announcement.getTitle(),
                "content", announcement.getContent(),
                "status", announcement.getStatus(),
                "publishedAt", announcement.getPublishedAt()
        );
    }
}
