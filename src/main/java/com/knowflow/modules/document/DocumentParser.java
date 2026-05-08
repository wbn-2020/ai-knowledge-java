package com.knowflow.modules.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

@Component
public class DocumentParser {
    public String parse(Path path, String fileType) throws IOException {
        return switch (fileType.toLowerCase()) {
            case "pdf" -> parsePdf(path);
            case "docx" -> parseDocx(path);
            case "txt", "md" -> Files.readString(path, StandardCharsets.UTF_8);
            default -> throw new IOException("不支持的文件类型");
        };
    }

    private String parsePdf(Path path) throws IOException {
        try (PDDocument document = PDDocument.load(path.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String parseDocx(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }
}
