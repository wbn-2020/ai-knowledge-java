package com.knowflow.modules.notice;

import java.time.LocalDateTime;

public record NoticeVO(Long id, String title, String content, Boolean readFlag, Boolean enabled, LocalDateTime createTime) {
    public static NoticeVO from(Notification n) {
        return new NoticeVO(n.getId(), n.getTitle(), n.getContent(), n.getReadFlag(), null, n.getCreateTime());
    }

    public static NoticeVO from(Announcement a) {
        return new NoticeVO(a.getId(), a.getTitle(), a.getContent(), null, a.getEnabled(), a.getCreateTime());
    }
}
