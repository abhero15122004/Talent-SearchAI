package com.talentmatch.auth.dto;

import com.talentmatch.security.UserPrincipal;
import com.talentmatch.user.entity.Role;

public record AuthUserDto(
        Long id,
        String email,
        String fullName,
        Role role,
        Long organizationId,
        String organizationName
) {
    public static AuthUserDto from(UserPrincipal principal) {
        return new AuthUserDto(
                principal.getId(),
                principal.getUsername(),
                principal.getFullName(),
                principal.getRole(),
                principal.getOrganizationId(),
                principal.getOrganizationName()
        );
    }
}
