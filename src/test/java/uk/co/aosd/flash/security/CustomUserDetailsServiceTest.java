package uk.co.aosd.flash.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import uk.co.aosd.flash.domain.User;
import uk.co.aosd.flash.domain.UserRole;
import uk.co.aosd.flash.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new CustomUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsername_whenFoundByUsername_returnsUserDetails() {
        final var userId = UUID.randomUUID();
        final var user = new User(userId, "johndoe", "john@example.com", "hashed", UserRole.USER, null);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));

        final UserDetails details = service.loadUserByUsername("johndoe");

        assertThat(details).isNotNull();
        assertThat(details.getUsername()).isEqualTo("johndoe");
        assertThat(details.getPassword()).isEqualTo("hashed");
        assertThat(details.getAuthorities()).hasSize(1);
        assertThat(details.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        verify(userRepository).findByUsername("johndoe");
    }

    @Test
    void loadUserByUsername_whenFoundByEmail_returnsUserDetails() {
        final var userId = UUID.randomUUID();
        final var user = new User(userId, "johndoe", "john@example.com", "hashed", UserRole.ADMIN_USER, null);
        when(userRepository.findByUsername("john@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        final UserDetails details = service.loadUserByUsername("john@example.com");

        assertThat(details).isNotNull();
        assertThat(details.getUsername()).isEqualTo("johndoe");
        assertThat(details.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN_USER");
    }

    @Test
    void loadUserByUsername_whenNotFound_throwsUsernameNotFoundException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("unknown"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User not found: unknown");
    }

    @Test
    void getUserIdByUsername_whenFound_returnsUserId() {
        final var userId = UUID.randomUUID();
        final var user = new User(userId, "johndoe", "john@example.com", "hashed", UserRole.USER, null);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));

        final UUID result = service.getUserIdByUsername("johndoe");

        assertThat(result).isEqualTo(userId);
    }

    @Test
    void getUserIdByUsername_whenNotFound_returnsNull() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

        final UUID result = service.getUserIdByUsername("unknown");

        assertThat(result).isNull();
    }
}
