package com.talentmatch.matching.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompareCandidatesRequest(
        @NotEmpty(message = "Candidate IDs list cannot be empty")
        @Size(min = 2, max = 5, message = "Candidate comparison requires between 2 and 5 candidates")
        List<Long> candidateIds
) {}
