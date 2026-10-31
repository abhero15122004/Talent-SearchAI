package com.talentmatch.workflow.dto;

import com.talentmatch.workflow.entity.ApplicationStage;
import jakarta.validation.constraints.NotNull;

public record UpdateStageRequest(
        @NotNull(message = "Target workflow stage is required")
        ApplicationStage stage,
        String decisionReason,
        String note
) {}
