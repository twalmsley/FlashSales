package uk.co.aosd.flash.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.co.aosd.flash.domain.UserRole;
import uk.co.aosd.flash.services.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String VALID_JWT = "valid.jwt.token";

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_withValidToken_setsAuthenticationAndInvokesChain() throws ServletException, IOException {
        final var userId = UUID.randomUUID();
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_JWT);
        when(jwtTokenProvider.validateToken(VALID_JWT)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(VALID_JWT)).thenReturn(userId);
        when(jwtTokenProvider.getUsernameFromToken(VALID_JWT)).thenReturn("user");
        when(jwtTokenProvider.getRoleFromToken(VALID_JWT)).thenReturn(UserRole.USER);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(getContext().getAuthentication()).isNotNull();
        assertThat(getContext().getAuthentication().getPrincipal()).isEqualTo(userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withNoToken_clearsContextAndInvokesChain() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withInvalidToken_clearsContextAndInvokesChain() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid");
        when(jwtTokenProvider.validateToken("invalid")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_whenProviderThrows_clearsContextAndInvokesChain() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_JWT);
        when(jwtTokenProvider.validateToken(VALID_JWT)).thenThrow(new RuntimeException("Token error"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
