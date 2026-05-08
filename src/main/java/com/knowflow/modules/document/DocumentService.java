package com.knowflow.modules.document;

import com.knowflow.common.BusinessException;
import com.knowflow.common.PageResponse;
import com.knowflow.common.enums.DocumentParseStatus;
import com.knowflow.common.enums.EmbeddingStatus;
import com.knowflow.common.enums.TaskStatus;
import com.knowflow.modules.knowledge.KnowledgeBase;
import com.knowflow.modules.knowledge.KnowledgeBaseRepository;
import com.knowflow.modules.knowledge.KnowledgeBaseService;
import com.knowflow.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {
    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "docx", "txt", "md");
    private final DocumentRepository documentRepository;
    private final DocumentProcessTaskRepository taskRepository;
    private final DocumentChunkRepository chunkRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentProcessService processService;
    private final Path uploadRoot;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentProcessTaskRepository taskRepository,
                           DocumentChunkRepository chunkRepository,
                           KnowledgeBaseRepository knowledgeBaseRepository,
                           KnowledgeBaseService knowledgeBaseService,
                           DocumentProcessService processService,
                           @Value("${knowflow.upload.root}") String uploadRoot) {
        this.documentRepository = documentRepository;
        this.taskRepository = taskRepository;
        this.chunkRepository = chunkRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeBaseService = knowledgeBaseService;
        this.processService = processService;
        this.uploadRoot = Path.of(uploadRoot);
    }

    @Transactional
    public DocumentVO upload(Long knowledgeBaseId, MultipartFile file) throws IOException {
        KnowledgeBase kb = knowledgeBaseService.requireOwned(knowledgeBaseId);
        String originalName = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        String fileType = extension(originalName);
        if (!ALLOWED_TYPES.contains(fileType)) {
            throw BusinessException.badRequest("仅支持 PDF、DOCX、TXT、MD 文件");
        }
        Files.createDirectories(uploadRoot);
        String storedName = UUID.randomUUID() + "." + fileType;
        Path storedPath = uploadRoot.resolve(storedName).toAbsolutePath().normalize();
        file.transferTo(storedPath);

        Document document = new Document();
        document.setUserId(SecurityUtils.getCurrentUserId());
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setName(originalName);
        document.setOriginalName(originalName);
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setFilePath(storedPath.toString());
        document.setParseStatus(DocumentParseStatus.PENDING);
        document.setEmbeddingStatus(EmbeddingStatus.PENDING);
        documentRepository.save(document);

        kb.setDocumentCount(kb.getDocumentCount() + 1);
        knowledgeBaseRepository.save(kb);

        DocumentProcessTask task = createTask(document);
        processService.processAsync(task.getId());
        return DocumentVO.from(document);
    }

    public PageResponse<DocumentVO> page(Long knowledgeBaseId, String keyword, int pageNo, int pageSize) {
        knowledgeBaseService.requireOwned(knowledgeBaseId);
        return PageResponse.of(documentRepository
                .findByUserIdAndKnowledgeBaseIdAndDeletedFalseAndNameContaining(
                        SecurityUtils.getCurrentUserId(),
                        knowledgeBaseId,
                        keyword == null ? "" : keyword,
                        PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime")))
                .map(DocumentVO::from));
    }

    public DocumentVO detail(Long id) {
        return DocumentVO.from(requireOwned(id));
    }

    @Transactional
    public void delete(Long id) {
        Document document = requireOwned(id);
        document.setDeleted(true);
        documentRepository.save(document);
        chunkRepository.deleteByDocumentId(id);
        knowledgeBaseRepository.findByIdAndUserIdAndDeletedFalse(document.getKnowledgeBaseId(), document.getUserId())
                .ifPresent(kb -> {
                    kb.setDocumentCount(Math.max(0, kb.getDocumentCount() - 1));
                    knowledgeBaseRepository.save(kb);
                });
    }

    @Transactional
    public DocumentVO retry(Long id) {
        Document document = requireOwned(id);
        chunkRepository.deleteByDocumentId(id);
        document.setParseStatus(DocumentParseStatus.PENDING);
        document.setEmbeddingStatus(EmbeddingStatus.PENDING);
        document.setErrorMessage(null);
        documentRepository.save(document);
        DocumentProcessTask task = createTask(document);
        processService.processAsync(task.getId());
        return DocumentVO.from(document);
    }

    public Document requireOwned(Long id) {
        return documentRepository.findByIdAndUserIdAndDeletedFalse(id, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> BusinessException.notFound("文档不存在"));
    }

    private DocumentProcessTask createTask(Document document) {
        DocumentProcessTask task = new DocumentProcessTask();
        task.setUserId(document.getUserId());
        task.setKnowledgeBaseId(document.getKnowledgeBaseId());
        task.setDocumentId(document.getId());
        task.setStatus(TaskStatus.PENDING);
        return taskRepository.save(task);
    }

    private String extension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw BusinessException.badRequest("文件缺少扩展名");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
