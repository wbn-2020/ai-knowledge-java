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
import java.util.List;
import java.util.stream.DoubleStream;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
public class DocumentProcessService {
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentProcessTaskRepository taskRepository;
    private final DocumentParser documentParser;
    private final TextChunker textChunker;
    private final EmbeddingClient embeddingClient;

    public DocumentProcessService(DocumentRepository documentRepository,
                                  DocumentChunkRepository chunkRepository,
                                  DocumentProcessTaskRepository taskRepository,
                                  DocumentParser documentParser,
                                  TextChunker textChunker,
                                  EmbeddingClient embeddingClient) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.taskRepository = taskRepository;
        this.documentParser = documentParser;
        this.textChunker = textChunker;
        this.embeddingClient = embeddingClient;
    }

    @Async
    @Transactional
    public void processAsync(Long taskId) {
        DocumentProcessTask task = taskRepository.selectById(taskId);
        Document document = documentRepository.selectById(task.getDocumentId());
        try {
            task.setStatus(TaskStatus.PROCESSING);
            document.setParseStatus(DocumentParseStatus.PARSING);
            document.setEmbeddingStatus(EmbeddingStatus.PROCESSING);
            taskRepository.updateById(task);
            documentRepository.updateById(document);

            chunkRepository.deleteByDocumentId(document.getId());
            String text = documentParser.parse(Path.of(document.getFilePath()), document.getFileType());
            List<String> chunks = textChunker.chunk(text);
            for (int i = 0; i < chunks.size(); i++) {
                String content = chunks.get(i);
                DocumentChunk chunk = new DocumentChunk();
                chunk.setUserId(document.getUserId());
                chunk.setKnowledgeBaseId(document.getKnowledgeBaseId());
                chunk.setDocumentId(document.getId());
                chunk.setChunkIndex(i);
                chunk.setContent(content);
                chunk.setTokenCount(Math.max(1, content.length() / 2));
                chunk.setEmbedding(serialize(embeddingClient.embed(content)));
                chunkRepository.insert(chunk);
            }
            task.setStatus(TaskStatus.SUCCESS);
            task.setFailReason(null);
            document.setParseStatus(DocumentParseStatus.SUCCESS);
            document.setEmbeddingStatus(EmbeddingStatus.SUCCESS);
            document.setErrorMessage(null);
        } catch (Exception ex) {
            task.setStatus(TaskStatus.FAILED);
            task.setFailReason(ex.getMessage());
            document.setParseStatus(DocumentParseStatus.FAILED);
            document.setEmbeddingStatus(EmbeddingStatus.FAILED);
            document.setErrorMessage(ex.getMessage());
        }
        taskRepository.updateById(task);
        documentRepository.updateById(document);
    }

    private String serialize(double[] vector) {
        return DoubleStream.of(vector)
                .mapToObj(v -> String.format("%.6f", v))
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }
}
