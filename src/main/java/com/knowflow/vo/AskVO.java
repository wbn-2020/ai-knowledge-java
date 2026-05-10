package com.knowflow.vo;

import java.util.List;

public record AskVO(
        Long sessionId,
        String question,
        String answer,
        String answerType,
        Boolean canUseGeneralAnswer,
        Long messageId,
        List<ReferenceVO> references,
        Boolean found,
        Boolean basedOnKnowledgeBase,
        String noAnswerReason
) {
}
