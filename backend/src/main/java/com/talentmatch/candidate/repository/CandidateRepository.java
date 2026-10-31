package com.talentmatch.candidate.repository;

import com.talentmatch.candidate.entity.Candidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    Page<Candidate> findByOrganizationId(Long organizationId, Pageable pageable);

    @Query("SELECT c FROM Candidate c WHERE c.organization.id = :orgId AND " +
            "(LOWER(c.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(c.primarySkills) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Candidate> searchCandidates(@Param("orgId") Long orgId, @Param("q") String query, Pageable pageable);

    Optional<Candidate> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<Candidate> findFirstByOrganizationIdAndEmail(Long organizationId, String email);

    java.util.List<Candidate> findAllByOrganizationId(Long organizationId);

    long countByOrganizationId(Long organizationId);
}
