package uk.co.aosd.flash.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;

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
     * Supports both JWT authentication (UUID principal) and form login (UserDetails
     * principal).
     * For form login, requires UserService to look up user ID from username.
     *
     * @return user ID
     * @throws IllegalStateException
     *             if user is not authenticated
     */
    public static UUID getCurrentUserId() {
        final Authentication authentication = securityStrategy.getContext().getAuthentication();

        // If authentication is null, check session for userId
        if (authentication == null) {
            final var session = org.springframework.web.context.request.RequestContextHolder
                .getRequestAttributes();
            if (session instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                final var request = ((org.springframework.web.context.request.ServletRequestAttributes) session)
                    .getRequest();
                final var userId = (UUID) request.getSession().getAttribute("userId");
                if (userId != null) {
                    return userId;
                }
            }
            throw new IllegalStateException(
                "User is not authenticated - authentication is null and userId is not available in session. Please ensure the user is properly authenticated or that userId is stored in the session.");
        }

        // Check if authentication is valid
        if (!authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            if (!authentication.isAuthenticated()) {
                throw new IllegalStateException("User is not authenticated - authentication.isAuthenticated is false");
            } else {
                throw new IllegalStateException("User is not authenticated - authentication.getPrincipal is null");
            }
        }

        // Handle JWT authentication (UUID principal)
        if (authentication.getPrincipal() instanceof UUID) {
            return (UUID) authentication.getPrincipal();
        }

        // Handle form login (UserDetails principal)
        if (authentication.getPrincipal() instanceof UserDetails) {
            // For form login, try to get user ID from session or look it up
            final var session = org.springframework.web.context.request.RequestContextHolder
                .getRequestAttributes();
            if (session instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                final var request = ((org.springframework.web.context.request.ServletRequestAttributes) session)
                    .getRequest();
                final var userId = (UUID) request.getSession().getAttribute("userId");
                if (userId != null) {
                    return userId;
                }
            }
            throw new IllegalStateException("Form login detected but user ID not in session. Use getCurrentUsername() and look up user ID.");
        }

        throw new IllegalStateException("Unexpected authentication principal type: " + authentication.getPrincipal().getClass().getName());
    }

    /**
     * Get the current authenticated user's username from SecurityContext.
     * Works with both JWT and form login.
     *
     * @return username
     * @throws IllegalStateException
     *             if user is not authenticated
     */
    public static String getCurrentUsername() {
        final Authentication authentication = securityStrategy.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        if (authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }

        // For JWT with UUID principal, we can't get username directly
        // Controllers using JWT should use getCurrentUserId() instead
        throw new IllegalStateException("Cannot get username from UUID principal. Use getCurrentUserId() for JWT authentication.");
    }

    /**
     * Get the current authenticated user's ID, or null if not authenticated.
     * Use for audit logging where the actor may be absent (e.g. background jobs).
     *
     * @return user ID, or null
     */
    public static UUID getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (final IllegalStateException e) {
            return null;
        }
    }

    /**
     * Get the current authenticated user's username, or null if not authenticated.
     *
     * @return username, or null
     */
    public static String getCurrentUsernameOrNull() {
        try {
            return getCurrentUsername();
        } catch (final IllegalStateException e) {
            return null;
        }
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
