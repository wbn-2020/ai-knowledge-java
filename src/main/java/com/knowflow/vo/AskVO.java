package com.knowflow.vo;

import java.util.List;

public record AskVO(Long sessionId, String question, String answer, Long messageId, List<ReferenceVO> references) {
}
