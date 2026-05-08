package com.knowflow.modules.knowledge;

import com.knowflow.common.BusinessException;
import com.knowflow.common.PageResponse;
import com.knowflow.modules.knowledge.dto.KnowledgeBaseRequest;
import com.knowflow.security.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
        repository.insert(kb);
        return KnowledgeBaseVO.from(kb);
    }

    public PageResponse<KnowledgeBaseVO> page(String keyword, int pageNo, int pageSize, String sortBy) {
        Page<KnowledgeBase> page = repository.findByUserIdAndDeletedFalseAndNameContaining(
                keyword == null ? "" : keyword,
                SecurityUtils.getCurrentUserId(),
                new Page<>(pageNo, pageSize));
        return PageResponse.of(convertPage(page, page.getRecords().stream().map(KnowledgeBaseVO::from).toList()));
    }

    public KnowledgeBaseVO detail(Long id) {
        return KnowledgeBaseVO.from(requireOwned(id));
    }

    @Transactional
    public KnowledgeBaseVO update(Long id, KnowledgeBaseRequest request) {
        KnowledgeBase kb = requireOwned(id);
        fill(kb, request);
        repository.updateById(kb);
        return KnowledgeBaseVO.from(kb);
    }

    @Transactional
    public void delete(Long id) {
        KnowledgeBase kb = requireOwned(id);
        kb.setDeleted(true);
        repository.updateById(kb);
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

    private Page<KnowledgeBaseVO> convertPage(Page<KnowledgeBase> source, java.util.List<KnowledgeBaseVO> records) {
        Page<KnowledgeBaseVO> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        target.setRecords(records);
        return target;
    }
}
