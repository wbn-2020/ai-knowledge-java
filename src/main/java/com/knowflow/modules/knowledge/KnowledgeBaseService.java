package com.knowflow.modules.knowledge;

import com.knowflow.common.BusinessException;
import com.knowflow.common.PageResponse;
import com.knowflow.modules.knowledge.dto.KnowledgeBaseRequest;
import com.knowflow.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeBaseService {
    private final KnowledgeBaseRepository repository;

    public KnowledgeBaseService(KnowledgeBaseRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public KnowledgeBaseVO create(KnowledgeBaseRequest request) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setUserId(SecurityUtils.getCurrentUserId());
        fill(kb, request);
        return KnowledgeBaseVO.from(repository.save(kb));
    }

    public PageResponse<KnowledgeBaseVO> page(String keyword, int pageNo, int pageSize, String sortBy) {
        Sort sort = "createTime".equals(sortBy)
                ? Sort.by(Sort.Direction.DESC, "createTime")
                : Sort.by(Sort.Direction.DESC, "updateTime");
        return PageResponse.of(repository
                .findByUserIdAndDeletedFalseAndNameContaining(keyword == null ? "" : keyword, SecurityUtils.getCurrentUserId(), PageRequest.of(pageNo - 1, pageSize, sort))
                .map(KnowledgeBaseVO::from));
    }

    public KnowledgeBaseVO detail(Long id) {
        return KnowledgeBaseVO.from(requireOwned(id));
    }

    @Transactional
    public KnowledgeBaseVO update(Long id, KnowledgeBaseRequest request) {
        KnowledgeBase kb = requireOwned(id);
        fill(kb, request);
        return KnowledgeBaseVO.from(repository.save(kb));
    }

    @Transactional
    public void delete(Long id) {
        KnowledgeBase kb = requireOwned(id);
        kb.setDeleted(true);
        repository.save(kb);
    }

    public KnowledgeBase requireOwned(Long id) {
        return repository.findByIdAndUserIdAndDeletedFalse(id, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> BusinessException.notFound("知识库不存在"));
    }

    private void fill(KnowledgeBase kb, KnowledgeBaseRequest request) {
        kb.setName(request.name());
        kb.setDescription(request.description());
        kb.setIcon(request.icon());
        kb.setCategory(request.category());
    }
}
