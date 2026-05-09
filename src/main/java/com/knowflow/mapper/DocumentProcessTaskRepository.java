package com.knowflow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.entity.DocumentProcessTask;
import com.knowflow.enums.TaskStatus;
import java.util.Optional;


public interface DocumentProcessTaskRepository extends BaseMapper<DocumentProcessTask> {
    default Page<DocumentProcessTask> findByDeletedFalse(Page<DocumentProcessTask> page) {
        return selectPage(page, new LambdaQueryWrapper<DocumentProcessTask>().orderByDesc(DocumentProcessTask::getCreateTime));
    }

    default Page<DocumentProcessTask> findByFilters(TaskStatus status, String taskType, String keyword, Page<DocumentProcessTask> page) {
        Long keywordAsDocId = parseLong(keyword);
        return selectPage(page, new LambdaQueryWrapper<DocumentProcessTask>()
                .eq(status != null, DocumentProcessTask::getStatus, status)
                .eq(taskType != null && !taskType.isBlank(), DocumentProcessTask::getTaskType, taskType)
                .and(keyword != null && !keyword.isBlank(),
                        q -> q.like(DocumentProcessTask::getFailReason, keyword)
                              .or(keywordAsDocId != null)
                              .eq(keywordAsDocId != null, DocumentProcessTask::getDocumentId, keywordAsDocId))
                .orderByDesc(DocumentProcessTask::getCreateTime));
    }

    default boolean existsActiveByDocumentId(Long documentId) {
        return selectCount(new LambdaQueryWrapper<DocumentProcessTask>()
                .eq(DocumentProcessTask::getDocumentId, documentId)
                .in(DocumentProcessTask::getStatus, TaskStatus.PENDING, TaskStatus.PROCESSING)) > 0;
    }

    default Optional<DocumentProcessTask> findLatestByDocumentId(Long documentId) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<DocumentProcessTask>()
                .eq(DocumentProcessTask::getDocumentId, documentId)
                .orderByDesc(DocumentProcessTask::getCreateTime)
                .last("limit 1")));
    }

    default void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        delete(new LambdaQueryWrapper<DocumentProcessTask>().eq(DocumentProcessTask::getKnowledgeBaseId, knowledgeBaseId));
    }

    private Long parseLong(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
