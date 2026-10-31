package com.talentmatch.workflow.dto;

import com.talentmatch.workflow.entity.ApplicationStage;
import java.math.BigDecimal;
import java.time.Instant;

public record JobApplicationDto(
        Long id,
        Long jobId,
        String jobTitle,
        Long candidateId,
        String candidateName,
        String candidateEmail,
        String candidatePhone,
        String candidateSkills,
        ApplicationStage stage,
        boolean shortlisted,
        String decisionReason,
        String assignedToUserName,
        BigDecimal latestMatchScore,
        Integer latestMatchRank,
        Boolean latestMatchEligible,
        Instant stageUpdatedAt,
        Instant createdAt
) {}
