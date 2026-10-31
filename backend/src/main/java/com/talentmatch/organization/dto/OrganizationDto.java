package com.talentmatch.organization.dto;

import com.talentmatch.organization.entity.Organization;

import java.time.LocalDateTime;

public record OrganizationDto(
        Long id,
        String name,
        String slug,
        String domain,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrganizationDto from(Organization org) {
        if (org == null) return null;
        return new OrganizationDto(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.getDomain(),
                org.isActive(),
                org.getCreatedAt(),
                org.getUpdatedAt()
        );
    }
}
