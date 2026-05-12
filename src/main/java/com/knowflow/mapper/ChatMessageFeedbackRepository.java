package com.knowflow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.entity.ChatMessageFeedback;
import java.util.Optional;

public interface ChatMessageFeedbackRepository extends BaseMapper<ChatMessageFeedback> {
    default Optional<ChatMessageFeedback> findByUserIdAndMessageId(Long userId, Long messageId) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<ChatMessageFeedback>()
                .eq(ChatMessageFeedback::getUserId, userId)
                .eq(ChatMessageFeedback::getMessageId, messageId)
                .last("limit 1")));
    }

    default void deleteByUserIdAndMessageId(Long userId, Long messageId) {
        delete(new LambdaQueryWrapper<ChatMessageFeedback>()
                .eq(ChatMessageFeedback::getUserId, userId)
                .eq(ChatMessageFeedback::getMessageId, messageId));
    }

    default Page<ChatMessageFeedback> pageForAdmin(String feedbackType,
                                                   String reason,
                                                   java.util.List<Long> userIds,
                                                   java.util.List<Long> messageIds,
                                                   String keyword,
                                                   int pageNo,
                                                   int pageSize) {
        return selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<ChatMessageFeedback>()
                .eq(feedbackType != null && !feedbackType.isBlank(), ChatMessageFeedback::getFeedbackType, feedbackType)
                .eq(reason != null && !reason.isBlank(), ChatMessageFeedback::getReason, reason)
                .in(userIds != null && !userIds.isEmpty(), ChatMessageFeedback::getUserId, userIds)
                .in(messageIds != null && !messageIds.isEmpty(), ChatMessageFeedback::getMessageId, messageIds)
                .like(keyword != null && !keyword.isBlank(), ChatMessageFeedback::getRemark, keyword)
                .orderByDesc(ChatMessageFeedback::getCreateTime));
    }
}
