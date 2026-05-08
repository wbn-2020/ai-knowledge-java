package com.knowflow.vo;

import java.util.List;

public record AskVO(Long sessionId, String answer, List<ReferenceVO> references) {
}
