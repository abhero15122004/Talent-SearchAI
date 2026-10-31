package com.talentmatch.resume.repository;

import com.talentmatch.resume.entity.BatchStatus;
import com.talentmatch.resume.entity.ResumeBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeBatchRepository extends JpaRepository<ResumeBatch, Long> {

    Page<ResumeBatch> findByOrganizationId(Long organizationId, Pageable pageable);

    Optional<ResumeBatch> findByIdAndOrganizationId(Long id, Long organizationId);

    @Query("SELECT b FROM ResumeBatch b LEFT JOIN FETCH b.files WHERE b.id = :id AND b.organization.id = :organizationId")
    Optional<ResumeBatch> findByIdAndOrganizationIdWithFiles(@Param("id") Long id, @Param("organizationId") Long organizationId);

    List<ResumeBatch> findByStatusIn(Collection<BatchStatus> statuses);
}
