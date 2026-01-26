package uk.co.aosd.flash.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

/**
 * Utility class for extracting security context information.
 */
public final class SecurityUtils {

    private static final SecurityContextHolderStrategy securityStrategy = SecurityContextHolder.getContextHolderStrategy();

    private SecurityUtils() {
        // Utility class
    }

    /**
     * Get the current authenticated user's ID from SecurityContext.
     *
     * @return user ID
     * @throws IllegalStateException
     *             if user is not authenticated
     */
    public static UUID getCurrentUserId() {
        final Authentication authentication = securityStrategy.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            if (authentication == null) {
                throw new IllegalStateException("User is not authenticated - authentication is null");
            } else if (!authentication.isAuthenticated()) {
                throw new IllegalStateException("User is not authenticated - authentication.isAuthenticated is false");
            } else {
                throw new IllegalStateException("User is not authenticated - authentication.getPrincipal is null");
            }
        }
        if (authentication.getPrincipal() instanceof UUID) {
            return (UUID) authentication.getPrincipal();
        }
        throw new IllegalStateException("Unexpected authentication principal type");
    }

    /**
     * Check if the current user is authenticated.
     *
     * @return true if authenticated
     */
    public static boolean isAuthenticated() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * Check if the current user has the specified role.
     *
     * @param role
     *            the role to check
     * @return true if user has the role
     */
    public static boolean hasRole(final String role) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
