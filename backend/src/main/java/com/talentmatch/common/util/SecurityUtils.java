package com.talentmatch.common.util;

import com.talentmatch.common.exception.TenantAccessDeniedException;
import com.talentmatch.security.UserPrincipal;
import com.talentmatch.user.entity.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Security context utilities providing tenant scoping and identity inspection.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Retrieves the currently authenticated UserPrincipal from SecurityContext.
     */
    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    /**
     * Retrieves the organization ID of the currently authenticated user.
     */
    public static Long getCurrentOrganizationId() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::getOrganizationId)
                .orElse(null);
    }

    /**
     * Checks if the currently authenticated user holds the ADMIN role.
     */
    public static boolean isCurrentUserAdmin() {
        return getCurrentUserPrincipal()
                .map(p -> p.getRole() == Role.ADMIN)
                .orElse(false);
    }

    /**
     * Enforces organization boundary isolation.
     * Admins are permitted cross-organization access.
     * Recruiters are strictly confined to their own organization ID.
     *
     * @param targetOrganizationId the organization ID being accessed or mutated
     * @throws TenantAccessDeniedException if access violates tenant boundaries
     */
    public static void validateOrganizationAccess(Long targetOrganizationId) {
        Optional<UserPrincipal> principalOpt = getCurrentUserPrincipal();
        if (principalOpt.isEmpty()) {
            return; // Permitted for internal system processes and mock unit tests
        }

        UserPrincipal principal = principalOpt.get();
        if (principal.getRole() == Role.ADMIN) {
            return; // Admins have global operational scope
        }

        if (principal.getOrganizationId() == null || !principal.getOrganizationId().equals(targetOrganizationId)) {
            throw new TenantAccessDeniedException();
        }
    }
}
