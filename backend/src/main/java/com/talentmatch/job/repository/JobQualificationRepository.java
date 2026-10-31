package com.talentmatch.job.repository;

import com.talentmatch.job.entity.JobQualification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobQualificationRepository extends JpaRepository<JobQualification, Long> {
    List<JobQualification> findByJobId(Long jobId);
    void deleteByJobId(Long jobId);
}
