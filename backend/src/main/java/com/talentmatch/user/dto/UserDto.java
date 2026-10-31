package com.talentmatch.user.dto;

import com.talentmatch.user.entity.Role;
import com.talentmatch.user.entity.User;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String email,
        String fullName,
        Role role,
        Long organizationId,
        String organizationName,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserDto from(User user) {
        if (user == null) return null;
        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        String orgName = user.getOrganization() != null ? user.getOrganization().getName() : null;

        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                orgId,
                orgName,
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
