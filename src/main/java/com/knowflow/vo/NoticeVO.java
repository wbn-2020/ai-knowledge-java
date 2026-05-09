package com.knowflow.vo;

import com.knowflow.entity.Announcement;
import com.knowflow.entity.Notification;
import java.time.LocalDateTime;


public record NoticeVO(Long id, String title, String content, Boolean readFlag, Boolean isRead, Boolean enabled, LocalDateTime createTime) {
    public static NoticeVO from(Notification n) {
        return new NoticeVO(n.getId(), n.getTitle(), n.getContent(), n.getReadFlag(), n.getReadFlag(), null, n.getCreateTime());
    }

    public static NoticeVO from(Announcement a) {
        return new NoticeVO(a.getId(), a.getTitle(), a.getContent(), null, null, a.getEnabled(), a.getCreateTime());
    }
}
