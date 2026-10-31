package com.talentmatch.audit.service;

import com.talentmatch.audit.dto.AuditLogDto;
import com.talentmatch.audit.entity.AuditLog;
import com.talentmatch.common.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    AuditLog recordEvent(Long organizationId, Long userId, String userEmail, String action,
                         String entityType, String entityId, String details, String ipAddress);

    PagedResponse<AuditLogDto> getAuditLogsForOrganization(Long organizationId, Pageable pageable);

    PagedResponse<AuditLogDto> getAllAuditLogs(Pageable pageable);
}
