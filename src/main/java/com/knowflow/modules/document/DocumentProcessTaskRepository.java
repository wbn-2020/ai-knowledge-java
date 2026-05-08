package com.knowflow.modules.document;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface DocumentProcessTaskRepository extends BaseMapper<DocumentProcessTask> {
    default Page<DocumentProcessTask> findByDeletedFalse(Page<DocumentProcessTask> page) {
        return selectPage(page, new LambdaQueryWrapper<DocumentProcessTask>().orderByDesc(DocumentProcessTask::getCreateTime));
    }

    default void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        delete(new LambdaQueryWrapper<DocumentProcessTask>().eq(DocumentProcessTask::getKnowledgeBaseId, knowledgeBaseId));
    }
}
