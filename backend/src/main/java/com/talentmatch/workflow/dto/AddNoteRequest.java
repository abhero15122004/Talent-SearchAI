package com.talentmatch.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record AddNoteRequest(
        @NotBlank(message = "Note text cannot be empty")
        String noteText,
        String decisionReason
) {}
