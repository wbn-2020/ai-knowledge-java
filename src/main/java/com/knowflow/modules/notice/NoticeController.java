package com.knowflow.modules.notice;

import com.knowflow.common.ApiResponse;
import com.knowflow.common.PageResponse;
import com.knowflow.modules.notice.dto.AnnouncementRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NoticeController {
    private final NoticeService service;

    public NoticeController(NoticeService service) {
        this.service = service;
    }

    @GetMapping("/notifications")
    public ApiResponse<PageResponse<NoticeVO>> notifications(@RequestParam(defaultValue = "1") int pageNo,
                                                             @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(service.notifications(pageNo, pageSize));
    }

    @GetMapping("/notifications/unread-count")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.ok(service.unreadCount());
    }

    @PutMapping("/notifications/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        service.markRead(id);
        return ApiResponse.ok();
    }

    @PutMapping("/notifications/read-all")
    public ApiResponse<Void> markAllRead() {
        service.markAllRead();
        return ApiResponse.ok();
    }

    @GetMapping("/announcements")
    public ApiResponse<PageResponse<NoticeVO>> publicAnnouncements(@RequestParam(defaultValue = "1") int pageNo,
                                                                   @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(service.publicAnnouncements(pageNo, pageSize));
    }

    @GetMapping("/admin/announcements")
    public ApiResponse<PageResponse<NoticeVO>> announcements(@RequestParam(defaultValue = "1") int pageNo,
                                                             @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(service.announcements(pageNo, pageSize));
    }

    @PostMapping("/admin/announcements")
    public ApiResponse<NoticeVO> saveAnnouncement(@Valid @RequestBody AnnouncementRequest request) {
        return ApiResponse.ok(service.saveAnnouncement(request));
    }

    @PutMapping("/admin/announcements/{id}")
    public ApiResponse<NoticeVO> updateAnnouncement(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest request) {
        return ApiResponse.ok(service.updateAnnouncement(id, request));
    }

    @DeleteMapping("/admin/announcements/{id}")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
        service.deleteAnnouncement(id);
        return ApiResponse.ok();
    }
}
