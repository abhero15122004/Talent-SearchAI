package com.talentmatch.audit.service;

import com.talentmatch.audit.dto.AuditLogDto;
import com.talentmatch.audit.entity.AuditLog;
import com.talentmatch.audit.repository.AuditLogRepository;
import com.talentmatch.common.dto.PagedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public AuditLog recordEvent(Long organizationId, Long userId, String userEmail, String action,
                                String entityType, String entityId, String details, String ipAddress) {
        AuditLog auditLog = new AuditLog(
                organizationId,
                userId,
                userEmail,
                action,
                entityType,
                entityId,
                details,
                ipAddress
        );
        log.info("Audit [org={}, user={}, action={}]", organizationId, userEmail, action);
        return auditLogRepository.save(auditLog);
    }

    @Override
    public PagedResponse<AuditLogDto> getAuditLogsForOrganization(Long organizationId, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findByOrganizationId(organizationId, pageable);
        return PagedResponse.from(page.map(AuditLogDto::from));
    }

    @Override
    public PagedResponse<AuditLogDto> getAllAuditLogs(Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findAllByOrderByTimestampDesc(pageable);
        return PagedResponse.from(page.map(AuditLogDto::from));
    }
}
