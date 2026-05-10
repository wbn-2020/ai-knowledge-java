package com.knowflow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.common.BusinessException;
import com.knowflow.common.PageResponse;
import com.knowflow.entity.Document;
import com.knowflow.entity.DocumentChunk;
import com.knowflow.entity.DocumentProcessTask;
import com.knowflow.entity.KnowledgeBase;
import com.knowflow.enums.DocumentParseStatus;
import com.knowflow.enums.EmbeddingStatus;
import com.knowflow.enums.TaskStatus;
import com.knowflow.mapper.ChatMessageReferenceRepository;
import com.knowflow.mapper.DocumentChunkRepository;
import com.knowflow.mapper.DocumentProcessTaskRepository;
import com.knowflow.mapper.DocumentRepository;
import com.knowflow.mapper.KnowledgeBaseRepository;
import com.knowflow.mapper.UserRepository;
import com.knowflow.security.SecurityUtils;
import com.knowflow.vo.DocumentChunkVO;
import com.knowflow.vo.DocumentVO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {
    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "docx", "txt", "md");
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentProcessTaskRepository taskRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ChatMessageReferenceRepository referenceRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final UserRepository userRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentProcessService processService;
    private final RuntimeConfigService runtimeConfigService;
    private final Path uploadRoot;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentProcessTaskRepository taskRepository,
                           DocumentChunkRepository chunkRepository,
                           ChatMessageReferenceRepository referenceRepository,
                           KnowledgeBaseRepository knowledgeBaseRepository,
                           UserRepository userRepository,
                           KnowledgeBaseService knowledgeBaseService,
                           DocumentProcessService processService,
                           RuntimeConfigService runtimeConfigService,
                           @Value("${knowflow.upload.root}") String uploadRoot) {
        this.documentRepository = documentRepository;
        this.taskRepository = taskRepository;
        this.chunkRepository = chunkRepository;
        this.referenceRepository = referenceRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.userRepository = userRepository;
        this.knowledgeBaseService = knowledgeBaseService;
        this.processService = processService;
        this.runtimeConfigService = runtimeConfigService;
        this.uploadRoot = Path.of(uploadRoot);
    }

    @Transactional
    public DocumentVO upload(Long knowledgeBaseId, MultipartFile file) throws IOException {
        if (knowledgeBaseId == null) {
            throw BusinessException.badRequest("请选择知识库后再上传文档");
        }
        KnowledgeBase kb = knowledgeBaseService.requireOwned(knowledgeBaseId);
        String originalName = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        String fileType = extension(originalName);
        if (!allowedTypes().contains(fileType)) {
            throw BusinessException.badRequest("仅支持 PDF、DOCX、TXT、MD 文件");
        }
        long maxBytes = runtimeConfigService.intValue("upload.maxFileSizeMb", 20) * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw BusinessException.badRequest("file size exceeds configured limit");
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
        documentRepository.insert(document);

        kb.setDocumentCount(kb.getDocumentCount() + 1);
        knowledgeBaseRepository.updateById(kb);

        DocumentProcessTask task = createTask(document);
        triggerProcessAfterCommit(task.getId(), document.getId());
        return enrichDocument(DocumentVO.from(document));
    }

    public PageResponse<DocumentVO> page(Long knowledgeBaseId,
                                         String keyword,
                                         DocumentParseStatus parseStatus,
                                         EmbeddingStatus embeddingStatus,
                                         int pageNo,
                                         int pageSize) {
        if (knowledgeBaseId != null) {
            knowledgeBaseService.requireOwned(knowledgeBaseId);
        }
        Long userId = SecurityUtils.getCurrentUserId();
        return PageResponse.of(documentRepository
                .findByUserIdAndFiltersAndDeletedFalse(
                        userId,
                        knowledgeBaseId,
                        keyword == null ? "" : keyword,
                        parseStatus,
                        embeddingStatus,
                        new Page<>(pageNo, pageSize))
                .convert(DocumentVO::from)
                .convert(this::enrichDocument));
    }

    public DocumentVO detail(Long id) {
        return enrichDocument(DocumentVO.from(requireOwned(id)));
    }

    public PageResponse<DocumentChunkVO> pageChunks(Long id, int pageNo, int pageSize) {
        Document document = requireOwned(id);
        Long userId = SecurityUtils.getCurrentUserId();
        return PageResponse.of(chunkRepository.findByUserIdAndDocumentIdOrderByChunkIndexAsc(
                        userId, document.getId(), new Page<>(pageNo, pageSize))
                .convert(DocumentChunkVO::from));
    }

    @Transactional
    public DocumentVO rename(Long id, String name) {
        Document document = requireOwned(id);
        document.setName(name);
        documentRepository.updateById(document);
        return enrichDocument(DocumentVO.from(document));
    }

    public String preview(Long id) {
        requireOwned(id);
        List<DocumentChunk> chunks = chunkRepository.findByDocumentIdAndDeletedFalseOrderByChunkIndexAsc(id);
        StringBuilder builder = new StringBuilder();
        for (DocumentChunk chunk : chunks.stream().limit(20).toList()) {
            builder.append(chunk.getContent()).append("\n\n");
        }
        return builder.toString();
    }

    public Path downloadPath(Long id) {
        Document document = requireOwned(id);
        Path path = Path.of(document.getFilePath()).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw BusinessException.notFound("file not found");
        }
        return path;
    }

    @Transactional
    public void delete(Long id) {
        Document document = requireOwned(id);
        doDelete(document, SecurityUtils.getCurrentUserId());
    }

    @Transactional
    public DocumentVO retry(Long id) {
        Document document = requireOwned(id);
        if (document.getKnowledgeBaseId() == null) {
            throw BusinessException.badRequest("文档未关联知识库，请重新上传到指定知识库");
        }
        chunkRepository.deleteByDocumentId(id);
        document.setParseStatus(DocumentParseStatus.PENDING);
        document.setEmbeddingStatus(EmbeddingStatus.PENDING);
        document.setErrorMessage(null);
        documentRepository.updateById(document);
        DocumentProcessTask task = createTask(document);
        triggerProcessAfterCommit(task.getId(), document.getId());
        return enrichDocument(DocumentVO.from(document));
    }

    public Document requireOwned(Long id) {
        return documentRepository.findByIdAndUserIdAndDeletedFalse(id, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> BusinessException.notFound("文档不存在或已删除"));
    }

    public DocumentVO adminDetail(Long id) {
        return documentRepository.findByIdAndDeletedFalse(id)
                .map(DocumentVO::from)
                .map(this::enrichDocument)
                .orElseThrow(() -> BusinessException.notFound("document not found"));
    }

    public PageResponse<DocumentChunkVO> adminPageChunks(Long id, int pageNo, int pageSize) {
        documentRepository.findByIdAndDeletedFalse(id).orElseThrow(() -> BusinessException.notFound("document not found"));
        return PageResponse.of(chunkRepository.findByDocumentIdOrderByChunkIndexAsc(id, new Page<>(pageNo, pageSize))
                .convert(DocumentChunkVO::from));
    }

    @Transactional
    public DocumentVO adminRetry(Long id) {
        Document document = documentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("document not found"));
        if (document.getKnowledgeBaseId() == null) {
            throw BusinessException.badRequest("文档未关联知识库，请重新上传到指定知识库");
        }
        chunkRepository.deleteByDocumentId(id);
        document.setParseStatus(DocumentParseStatus.PENDING);
        document.setEmbeddingStatus(EmbeddingStatus.PENDING);
        document.setErrorMessage(null);
        documentRepository.updateById(document);
        DocumentProcessTask task = createTask(document);
        triggerProcessAfterCommit(task.getId(), document.getId());
        return enrichDocument(DocumentVO.from(document));
    }

    @Transactional
    public void adminDelete(Long id) {
        Document document = documentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("document not found"));
        doDelete(document, SecurityUtils.getCurrentUserId());
    }

    private void doDelete(Document document, Long operatorUserId) {
        int documentDeletedUpdatedRows = documentRepository.deleteById(document.getId());
        if (documentDeletedUpdatedRows <= 0) {
            throw BusinessException.notFound("文档不存在或已删除");
        }

        int chunkDeletedUpdatedRows = chunkRepository.deleteByDocumentId(document.getId());
        int referenceDeletedUpdatedRows = referenceRepository.deleteByDocumentId(document.getId());

        try {
            Files.deleteIfExists(Path.of(document.getFilePath()));
        } catch (IOException ignored) {
        }

        long recalculatedDocumentCount = documentRepository.countByKnowledgeBaseIdAndDeletedFalse(document.getKnowledgeBaseId());
        knowledgeBaseRepository.findByIdAndDeletedFalse(document.getKnowledgeBaseId())
                .ifPresent(kb -> {
                    kb.setDocumentCount((int) recalculatedDocumentCount);
                    knowledgeBaseRepository.updateById(kb);
                });

        log.info("Document deleted: documentId={}, userId={}, knowledgeBaseId={}, documentDeletedUpdatedRows={}, chunkDeletedUpdatedRows={}, referenceDeletedUpdatedRows={}, recalculatedDocumentCount={}",
                document.getId(),
                operatorUserId,
                document.getKnowledgeBaseId(),
                documentDeletedUpdatedRows,
                chunkDeletedUpdatedRows,
                referenceDeletedUpdatedRows,
                recalculatedDocumentCount);
    }

    private DocumentProcessTask createTask(Document document) {
        DocumentProcessTask task = new DocumentProcessTask();
        task.setUserId(document.getUserId());
        task.setKnowledgeBaseId(document.getKnowledgeBaseId());
        task.setDocumentId(document.getId());
        task.setTaskType("DOCUMENT_PARSE");
        task.setStatus(TaskStatus.PENDING);
        taskRepository.insert(task);
        if (task.getId() == null) {
            throw BusinessException.badRequest("document process task id not generated");
        }
        return task;
    }

    private void triggerProcessAfterCommit(Long taskId, Long documentId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("No active transaction sync, process task immediately. taskId={}, documentId={}", taskId, documentId);
            processService.processAsync(taskId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                processService.processAsync(taskId);
            }
        });
    }

    private String extension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw BusinessException.badRequest("文件缺少扩展名");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private Set<String> allowedTypes() {
        String configured = runtimeConfigService.value("upload.allowedTypes");
        if (!StringUtils.hasText(configured)) {
            return ALLOWED_TYPES;
        }
        return java.util.Arrays.stream(configured.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toSet());
    }

    private DocumentVO enrichDocument(DocumentVO base) {
        String knowledgeBaseName = null;
        if (base.knowledgeBaseId() != null) {
            knowledgeBaseName = knowledgeBaseRepository.findByIdAndDeletedFalse(base.knowledgeBaseId())
                    .map(KnowledgeBase::getName)
                    .orElse(null);
        }
        Long uid = base.userId() != null ? base.userId() : base.uploaderId();
        String uploaderName = null;
        if (uid != null) {
            uploaderName = userRepository.findByIdAndDeletedFalse(uid)
                    .map(user -> StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername())
                    .orElse(null);
        }
        Long chunkCount = chunkRepository.countByDocumentId(base.id());
        return DocumentVO.enrich(base, knowledgeBaseName, uploaderName, chunkCount);
    }
}
