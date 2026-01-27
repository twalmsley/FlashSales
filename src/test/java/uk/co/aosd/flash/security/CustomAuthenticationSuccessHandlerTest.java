package uk.co.aosd.flash.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import uk.co.aosd.flash.domain.UserRole;
import uk.co.aosd.flash.repository.UserRepository;

/**
 * Unit tests for CustomAuthenticationSuccessHandler.
 * Verifies that SecurityContext (including authorities) is persisted to the session
 * so that role-based UI (e.g. sec:authorize="hasRole('ADMIN_USER')") works after redirect.
 */
@ExtendWith(MockitoExtension.class)
class CustomAuthenticationSuccessHandlerTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    private SecurityContextRepository securityContextRepository;
    private CustomAuthenticationSuccessHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        securityContextRepository = new HttpSessionSecurityContextRepository();
        handler = new CustomAuthenticationSuccessHandler(userRepository, securityContextRepository);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void persistsSecurityContextToSessionSoAdminRoleIsAvailableOnNextRequest() throws Exception {
        final var adminUser = new uk.co.aosd.flash.domain.User(
            ADMIN_USER_ID,
            ADMIN_USERNAME,
            "admin@example.com",
            "hashed",
            UserRole.ADMIN_USER,
            null);
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));

        final var userDetails = User.builder()
            .username(ADMIN_USERNAME)
            .password("ignored")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN_USER")))
            .build();
        final Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        handler.onAuthenticationSuccess(request, response, authentication);

        final var storedContext = request.getSession().getAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(storedContext).isNotNull();
        assertThat(storedContext).isInstanceOf(org.springframework.security.core.context.SecurityContext.class);

        final var context = (org.springframework.security.core.context.SecurityContext) storedContext;
        assertThat(context.getAuthentication()).isNotNull();
        assertThat(context.getAuthentication().getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_ADMIN_USER");

        assertThat(response.getRedirectedUrl()).isEqualTo("/");
        verify(userRepository).findByUsername(ADMIN_USERNAME);
        assertThat(request.getSession().getAttribute("userId")).isEqualTo(ADMIN_USER_ID);
    }

    @Test
    void persistsSecurityContextForRegularUser() throws Exception {
        final var regularUser = new uk.co.aosd.flash.domain.User(
            UUID.randomUUID(),
            "user",
            "user@example.com",
            "hashed",
            UserRole.USER,
            null);
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(regularUser));

        final var userDetails = User.builder()
            .username("user")
            .password("ignored")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();
        final Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        handler.onAuthenticationSuccess(request, response, authentication);

        final var storedContext = request.getSession().getAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(storedContext).isNotNull();
        final var context = (org.springframework.security.core.context.SecurityContext) storedContext;
        assertThat(context.getAuthentication().getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_USER");
    }

    @Test
    void stillPersistsSecurityContextWhenUserNotFoundInRepository() throws Exception {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        final var userDetails = User.builder()
            .username("orphan")
            .password("ignored")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN_USER")))
            .build();
        final Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        handler.onAuthenticationSuccess(request, response, authentication);

        final var storedContext = request.getSession().getAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(storedContext).isNotNull();
        final var context = (org.springframework.security.core.context.SecurityContext) storedContext;
        assertThat(context.getAuthentication().getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_ADMIN_USER");
    }
}
