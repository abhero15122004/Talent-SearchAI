package com.talentmatch.workflow.dto;

import java.time.Instant;

public record JobApplicationNoteDto(
        Long id,
        Long applicationId,
        String authorName,
        String authorEmail,
        String noteText,
        String stageAtCreation,
        String decisionReason,
        Instant createdAt
) {}
