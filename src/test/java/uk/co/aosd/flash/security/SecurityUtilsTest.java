package uk.co.aosd.flash.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * Unit tests for SecurityUtils.
 * Clears SecurityContextHolder after each test to avoid leaking state.
 */
@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserId_whenAuthenticationNull_throwsIllegalStateException() {
        SecurityContextHolder.getContext().setAuthentication(null);

        assertThatThrownBy(SecurityUtils::getCurrentUserId)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not authenticated");
    }

    @Test
    void getCurrentUserId_whenNotAuthenticated_throwsIllegalStateException() {
        final var auth = new UsernamePasswordAuthenticationToken("principal", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(SecurityUtils::getCurrentUserId)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getCurrentUserId_whenPrincipalIsUuid_returnsUserId() {
        final var userId = UUID.randomUUID();
        final var auth = new UsernamePasswordAuthenticationToken(userId, null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        final UUID result = SecurityUtils.getCurrentUserId();

        assertThat(result).isEqualTo(userId);
    }

    @Test
    void getCurrentUserId_whenUserDetailsPrincipalWithoutSession_throwsIllegalStateException() {
        final var userDetails = User.builder()
            .username("user")
            .password("hashed")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();
        final var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(SecurityUtils::getCurrentUserId)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("user ID not in session");
    }

    @Test
    void getCurrentUsername_whenUserDetailsPrincipal_returnsUsername() {
        final var userDetails = User.builder()
            .username("johndoe")
            .password("hashed")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();
        final var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        final String result = SecurityUtils.getCurrentUsername();

        assertThat(result).isEqualTo("johndoe");
    }

    @Test
    void getCurrentUsername_whenNotAuthenticated_throwsIllegalStateException() {
        SecurityContextHolder.getContext().setAuthentication(null);

        assertThatThrownBy(SecurityUtils::getCurrentUsername)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not authenticated");
    }

    @Test
    void getCurrentUsername_whenUuidPrincipal_throwsIllegalStateException() {
        final var auth = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(SecurityUtils::getCurrentUsername)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot get username from UUID principal");
    }

    @Test
    void isAuthenticated_whenNull_returnsFalse() {
        SecurityContextHolder.getContext().setAuthentication(null);

        assertThat(SecurityUtils.isAuthenticated()).isFalse();
    }

    @Test
    void isAuthenticated_whenAuthenticated_returnsTrue() {
        final var auth = new UsernamePasswordAuthenticationToken("principal", null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(SecurityUtils.isAuthenticated()).isTrue();
    }

    @Test
    void hasRole_whenNullAuth_returnsFalse() {
        SecurityContextHolder.getContext().setAuthentication(null);

        assertThat(SecurityUtils.hasRole("ADMIN_USER")).isFalse();
    }

    @Test
    void hasRole_whenUserHasRole_returnsTrue() {
        final var auth = new UsernamePasswordAuthenticationToken("principal", null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(SecurityUtils.hasRole("ADMIN_USER")).isTrue();
    }

    @Test
    void hasRole_whenUserDoesNotHaveRole_returnsFalse() {
        final var auth = new UsernamePasswordAuthenticationToken("principal", null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(SecurityUtils.hasRole("ADMIN_USER")).isFalse();
    }
}
