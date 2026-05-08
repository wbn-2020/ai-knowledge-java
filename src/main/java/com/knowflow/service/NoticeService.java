package com.knowflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.common.BusinessException;
import com.knowflow.common.PageResponse;
import com.knowflow.dto.AnnouncementRequest;
import com.knowflow.entity.Announcement;
import com.knowflow.entity.Notification;
import com.knowflow.mapper.AnnouncementRepository;
import com.knowflow.mapper.NotificationRepository;
import com.knowflow.security.SecurityUtils;
import com.knowflow.vo.NoticeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class NoticeService {
    private final NotificationRepository notificationRepository;
    private final AnnouncementRepository announcementRepository;

    public NoticeService(NotificationRepository notificationRepository, AnnouncementRepository announcementRepository) {
        this.notificationRepository = notificationRepository;
        this.announcementRepository = announcementRepository;
    }

    public PageResponse<NoticeVO> notifications(int pageNo, int pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        return PageResponse.of(notificationRepository.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<Notification>().eq(Notification::getUserId, userId).orderByDesc(Notification::getCreateTime)).convert(NoticeVO::from));
    }

    public long unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return notificationRepository.selectCount(new LambdaQueryWrapper<Notification>().eq(Notification::getUserId, userId).eq(Notification::getReadFlag, false));
    }

    @Transactional
    public void markRead(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Notification notification = notificationRepository.selectOne(new LambdaQueryWrapper<Notification>().eq(Notification::getId, id).eq(Notification::getUserId, userId).last("limit 1"));
        if (notification == null) {
            throw BusinessException.notFound("notification not found");
        }
        notification.setReadFlag(true);
        notificationRepository.updateById(notification);
    }

    @Transactional
    public void markAllRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationRepository.selectList(new LambdaQueryWrapper<Notification>().eq(Notification::getUserId, userId).eq(Notification::getReadFlag, false))
                .forEach(item -> {
                    item.setReadFlag(true);
                    notificationRepository.updateById(item);
                });
    }

    public PageResponse<NoticeVO> announcements(int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return PageResponse.of(announcementRepository.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<Announcement>().orderByDesc(Announcement::getCreateTime)).convert(NoticeVO::from));
    }

    public PageResponse<NoticeVO> publicAnnouncements(int pageNo, int pageSize) {
        return PageResponse.of(announcementRepository.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getEnabled, true)
                        .orderByDesc(Announcement::getCreateTime)).convert(NoticeVO::from));
    }

    @Transactional
    public NoticeVO saveAnnouncement(AnnouncementRequest request) {
        SecurityUtils.requireAdmin();
        Announcement announcement = new Announcement();
        announcement.setTitle(request.title());
        announcement.setContent(request.content());
        announcement.setEnabled(request.enabled() == null || request.enabled());
        announcementRepository.insert(announcement);
        return NoticeVO.from(announcement);
    }

    @Transactional
    public NoticeVO updateAnnouncement(Long id, AnnouncementRequest request) {
        SecurityUtils.requireAdmin();
        Announcement announcement = announcementRepository.selectById(id);
        if (announcement == null) {
            throw BusinessException.notFound("announcement not found");
        }
        announcement.setTitle(request.title());
        announcement.setContent(request.content());
        announcement.setEnabled(request.enabled() == null || request.enabled());
        announcementRepository.updateById(announcement);
        return NoticeVO.from(announcement);
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        SecurityUtils.requireAdmin();
        announcementRepository.deleteById(id);
    }
}
