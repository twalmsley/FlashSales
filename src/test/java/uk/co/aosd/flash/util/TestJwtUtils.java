package uk.co.aosd.flash.util;

import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import uk.co.aosd.flash.domain.UserRole;

/**
 * Utility class for creating test JWT authentication contexts.
 */
public class TestJwtUtils {

    /**
     * Set up a test security context with a user ID and role.
     *
     * @param userId the user ID
     * @param role   the user role
     */
    public static void setSecurityContext(final UUID userId, final UserRole role) {
        final var authorities = new java.util.ArrayList<org.springframework.security.core.GrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));

        final var authentication = new UsernamePasswordAuthenticationToken(
            userId,
            null,
            authorities
        );

        final SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    /**
     * Clear the security context.
     */
    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Security context factory for use with @WithMockUser-like annotations.
     */
    public static class TestSecurityContextFactory implements WithSecurityContextFactory<WithMockJwtUser> {
        @Override
        public SecurityContext createSecurityContext(final WithMockJwtUser annotation) {
            final SecurityContext context = SecurityContextHolder.createEmptyContext();
            final UUID userId = UUID.fromString(annotation.userId());
            final UserRole role = annotation.role();

            final var authorities = new java.util.ArrayList<org.springframework.security.core.GrantedAuthority>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));

            final var authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                authorities
            );

            context.setAuthentication(authentication);
            return context;
        }
    }
}
