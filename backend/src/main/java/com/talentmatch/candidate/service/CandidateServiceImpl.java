package com.talentmatch.candidate.service;

import com.talentmatch.candidate.dto.CandidateDetailResponse;
import com.talentmatch.candidate.dto.CandidateSummaryResponse;
import com.talentmatch.candidate.entity.Candidate;
import com.talentmatch.candidate.repository.CandidateRepository;
import com.talentmatch.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;

    public CandidateServiceImpl(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    @Override
    public Page<CandidateSummaryResponse> listCandidates(Long organizationId, String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return candidateRepository.searchCandidates(organizationId, search.trim(), pageable)
                    .map(CandidateSummaryResponse::fromEntity);
        }
        return candidateRepository.findByOrganizationId(organizationId, pageable)
                .map(CandidateSummaryResponse::fromEntity);
    }

    @Override
    public CandidateDetailResponse getCandidateById(Long candidateId, Long organizationId) {
        Candidate candidate = candidateRepository.findByIdAndOrganizationId(candidateId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", candidateId));
        return CandidateDetailResponse.fromEntity(candidate);
    }
}
