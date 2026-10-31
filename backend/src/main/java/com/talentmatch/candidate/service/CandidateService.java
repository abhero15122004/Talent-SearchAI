package com.talentmatch.candidate.service;

import com.talentmatch.candidate.dto.CandidateDetailResponse;
import com.talentmatch.candidate.dto.CandidateSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CandidateService {

    Page<CandidateSummaryResponse> listCandidates(Long organizationId, String search, Pageable pageable);

    CandidateDetailResponse getCandidateById(Long candidateId, Long organizationId);
}
