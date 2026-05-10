package com.knowflow.service;

import com.knowflow.entity.Document;
import com.knowflow.entity.DocumentChunk;
import com.knowflow.entity.DocumentProcessTask;
import com.knowflow.enums.DocumentParseStatus;
import com.knowflow.enums.EmbeddingStatus;
import com.knowflow.enums.TaskStatus;
import com.knowflow.infrastructure.ai.EmbeddingClient;
import com.knowflow.infrastructure.document.DocumentParser;
import com.knowflow.infrastructure.document.TextChunker;
import com.knowflow.mapper.DocumentChunkRepository;
import com.knowflow.mapper.DocumentProcessTaskRepository;
import com.knowflow.mapper.DocumentRepository;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentProcessService {
    private static final Logger log = LoggerFactory.getLogger(DocumentProcessService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentProcessTaskRepository taskRepository;
    private final DocumentParser documentParser;
    private final TextChunker textChunker;
    private final EmbeddingClient embeddingClient;
    private final LogService logService;

    public DocumentProcessService(DocumentRepository documentRepository,
                                  DocumentChunkRepository chunkRepository,
                                  DocumentProcessTaskRepository taskRepository,
                                  DocumentParser documentParser,
                                  TextChunker textChunker,
                                  EmbeddingClient embeddingClient,
                                  LogService logService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.taskRepository = taskRepository;
        this.documentParser = documentParser;
        this.textChunker = textChunker;
        this.embeddingClient = embeddingClient;
        this.logService = logService;
    }

    @Async
    @Transactional
    public void processAsync(Long taskId) {
        DocumentProcessTask task = null;
        Document document = null;
        long processStart = System.currentTimeMillis();
        List<String> logs = new ArrayList<>();
        try {
            task = taskRepository.selectById(taskId);
            if (task == null) {
                log.error("Document process task not found, taskId={}", taskId);
                return;
            }
            appendLog(logs, "创建解析任务");

            document = documentRepository.selectById(task.getDocumentId());
            if (document == null) {
                task.setStatus(TaskStatus.FAILED);
                task.setFailReason("document not found, documentId=" + task.getDocumentId());
                appendLog(logs, "解析失败：文档不存在");
                appendLog(logs, "任务结束：FAILED");
                return;
            }
            if (document.getKnowledgeBaseId() == null) {
                String reason = "文档未关联知识库，无法继续解析";
                task.setStatus(TaskStatus.FAILED);
                task.setFailReason(reason);
                document.setParseStatus(DocumentParseStatus.FAILED);
                document.setEmbeddingStatus(EmbeddingStatus.FAILED);
                document.setErrorMessage(reason);
                appendLog(logs, "开始解析文档");
                appendLog(logs, "解析失败：" + reason);
                appendLog(logs, "任务结束：FAILED");
                return;
            }

            task.setStatus(TaskStatus.PROCESSING);
            task.setStartedAt(LocalDateTime.now());
            document.setParseStatus(DocumentParseStatus.PARSING);
            document.setEmbeddingStatus(EmbeddingStatus.PROCESSING);
            appendLog(logs, "开始解析文档");
            taskRepository.updateById(task);
            documentRepository.updateById(document);

            chunkRepository.deleteByDocumentId(document.getId());
            String text = documentParser.parse(Path.of(document.getFilePath()), document.getFileType());
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("文档解析文本为空");
            }
            appendLog(logs, "文本解析成功");

            List<String> chunks = textChunker.chunk(text);
            if (chunks == null || chunks.isEmpty()) {
                throw new IllegalStateException("文档切片数量为 0");
            }
            appendLog(logs, "切片数量：" + chunks.size());
            appendLog(logs, "开始向量化");

            long embeddingStart = System.currentTimeMillis();
            int inputTokens = 0;
            int insertedChunkCount = 0;
            for (int i = 0; i < chunks.size(); i++) {
                String content = chunks.get(i);
                if (content == null || content.isBlank()) {
                    continue;
                }
                inputTokens += Math.max(1, content.length() / 4);
                DocumentChunk chunk = new DocumentChunk();
                chunk.setUserId(document.getUserId());
                chunk.setKnowledgeBaseId(document.getKnowledgeBaseId());
                chunk.setDocumentId(document.getId());
                chunk.setChunkIndex(i);
                chunk.setContent(content);
                chunk.setTokenCount(Math.max(1, content.length() / 2));
                chunk.setEmbedding(serialize(embeddingClient.embed(content)));
                chunkRepository.insert(chunk);
                insertedChunkCount++;
            }
            if (insertedChunkCount <= 0) {
                throw new IllegalStateException("文档切片保存失败，数量为 0");
            }
            appendLog(logs, "向量化成功");

            long embeddingDuration = System.currentTimeMillis() - embeddingStart;
            logService.recordAiCall(
                    document.getUserId(),
                    document.getKnowledgeBaseId(),
                    null,
                    embeddingClient.getClass().getSimpleName(),
                    "EMBEDDING",
                    "LOCAL",
                    "EMBEDDING",
                    embeddingDuration,
                    true,
                    null,
                    inputTokens,
                    null
            );
            task.setStatus(TaskStatus.SUCCESS);
            task.setFailReason(null);
            appendLog(logs, "任务完成");
            document.setParseStatus(DocumentParseStatus.SUCCESS);
            document.setEmbeddingStatus(EmbeddingStatus.SUCCESS);
            document.setErrorMessage(null);
        } catch (Exception ex) {
            log.error("Document process failed, taskId={}", taskId, ex);
            if (document != null) {
                long duration = Math.max(0L, System.currentTimeMillis() - processStart);
                logService.recordAiCall(
                        document.getUserId(),
                        document.getKnowledgeBaseId(),
                        null,
                        embeddingClient.getClass().getSimpleName(),
                        "EMBEDDING",
                        "LOCAL",
                        "EMBEDDING",
                        duration,
                        false,
                        ex.getMessage(),
                        null,
                        null
                );
            }
            if (task != null) {
                task.setStatus(TaskStatus.FAILED);
                task.setFailReason(ex.getMessage());
                appendLog(logs, "解析失败：" + ex.getMessage());
                appendLog(logs, "任务结束：FAILED");
            }
            if (document != null) {
                document.setParseStatus(DocumentParseStatus.FAILED);
                document.setEmbeddingStatus(EmbeddingStatus.FAILED);
                document.setErrorMessage(ex.getMessage());
            }
        } finally {
            if (task != null) {
                finalizeTask(task, processStart, logs);
                taskRepository.updateById(task);
            }
            if (document != null) {
                documentRepository.updateById(document);
            }
        }
    }

    private String serialize(double[] vector) {
        return DoubleStream.of(vector)
                .mapToObj(v -> String.format("%.6f", v))
                .collect(Collectors.joining(","));
    }

    private void appendLog(List<String> logs, String message) {
        logs.add(LocalDateTime.now() + " " + message);
    }

    private void finalizeTask(DocumentProcessTask task, long processStart, List<String> logs) {
        if (task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now());
        }
        task.setFinishedAt(LocalDateTime.now());
        task.setDurationMs(Math.max(0L, System.currentTimeMillis() - processStart));
        task.setLogsJson(logs == null || logs.isEmpty() ? "" : String.join("\n", logs));
    }
}
