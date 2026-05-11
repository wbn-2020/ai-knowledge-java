package com.knowflow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.entity.DocumentProcessTask;
import com.knowflow.enums.TaskStatus;
import java.util.Optional;


public interface DocumentProcessTaskRepository extends BaseMapper<DocumentProcessTask> {
    default Page<DocumentProcessTask> findByDeletedFalse(Page<DocumentProcessTask> page) {
        return selectPage(page, new LambdaQueryWrapper<DocumentProcessTask>().orderByDesc(DocumentProcessTask::getCreateTime));
    }

    default Page<DocumentProcessTask> findByFilters(TaskStatus status,
                                                    String taskType,
                                                    Long documentId,
                                                    String keyword,
                                                    java.util.List<Long> matchedDocumentIds,
                                                    Page<DocumentProcessTask> page) {
        Long keywordAsDocId = parseLong(keyword);
        QueryWrapper<DocumentProcessTask> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(status != null, DocumentProcessTask::getStatus, status)
                .eq(taskType != null && !taskType.isBlank(), DocumentProcessTask::getTaskType, taskType)
                .eq(documentId != null, DocumentProcessTask::getDocumentId, documentId);

        if (keyword != null && !keyword.isBlank()) {
            String lowerKeyword = "%" + keyword.toLowerCase() + "%";
            wrapper.and(q -> q
                    .apply("lower(ifnull(document_name_snapshot,'')) like {0}", lowerKeyword)
                    .or()
                    .apply("lower(ifnull(fail_reason,'')) like {0}", lowerKeyword)
                    .or()
                    .apply("lower(ifnull(logs_json,'')) like {0}", lowerKeyword)
                    .or()
                    .apply("lower(ifnull(task_type,'')) like {0}", lowerKeyword)
                    .or(keywordAsDocId != null)
                    .eq(keywordAsDocId != null, "document_id", keywordAsDocId)
                    .or(matchedDocumentIds != null && !matchedDocumentIds.isEmpty())
                    .in(matchedDocumentIds != null && !matchedDocumentIds.isEmpty(), "document_id", matchedDocumentIds)
            );
        }
        wrapper.orderByDesc("create_time");
        return selectPage(page, wrapper);
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
