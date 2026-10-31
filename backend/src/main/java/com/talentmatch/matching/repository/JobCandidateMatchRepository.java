package com.talentmatch.matching.repository;

import com.talentmatch.matching.entity.JobCandidateMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobCandidateMatchRepository extends JpaRepository<JobCandidateMatch, Long> {

    List<JobCandidateMatch> findByMatchRunIdAndOrganizationIdOrderByRankPositionAsc(Long matchRunId, Long organizationId);

    Optional<JobCandidateMatch> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<JobCandidateMatch> findByMatchRunIdAndCandidateIdAndOrganizationId(Long matchRunId, Long candidateId, Long organizationId);

    long countByOrganizationId(Long organizationId);
}
