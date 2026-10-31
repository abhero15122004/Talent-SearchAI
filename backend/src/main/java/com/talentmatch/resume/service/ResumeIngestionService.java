package com.talentmatch.resume.service;

import com.talentmatch.resume.dto.BatchResponse;
import com.talentmatch.resume.dto.BatchSummaryResponse;
import com.talentmatch.resume.dto.BatchUploadResponse;
import com.talentmatch.resume.dto.ResumeFileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeIngestionService {

    BatchUploadResponse uploadZipArchive(MultipartFile zipFile, Long organizationId, Long userId);

    BatchUploadResponse uploadFiles(List<MultipartFile> files, String batchName, Long organizationId, Long userId);

    BatchResponse getBatchById(Long batchId, Long organizationId);

    Page<BatchSummaryResponse> listBatches(Long organizationId, Pageable pageable);

    ResumeFileResponse retryFile(Long fileId, Long organizationId);
}
