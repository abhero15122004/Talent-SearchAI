package com.talentmatch.audit.dto;

import com.talentmatch.audit.entity.AuditLog;

import java.time.LocalDateTime;

/**
 * DTO representing an audit log entry.
 */
public record AuditLogDto(
        Long id,
        Long organizationId,
        Long userId,
        String userEmail,
        String action,
        String entityType,
        String entityId,
        String details,
        String ipAddress,
        LocalDateTime timestamp
) {
    public static AuditLogDto from(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getOrganizationId(),
                log.getUserId(),
                log.getUserEmail(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getIpAddress(),
                log.getTimestamp()
        );
    }
}
