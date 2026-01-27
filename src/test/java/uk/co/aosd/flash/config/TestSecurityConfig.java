package uk.co.aosd.flash.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.co.aosd.flash.security.JwtAuthenticationEntryPoint;
import uk.co.aosd.flash.security.JwtAuthenticationFilter;
import uk.co.aosd.flash.services.JwtTokenProvider;

/**
 * Test security configuration that disables security for @WebMvcTest.
 * Provides all security beans needed for tests since SecurityConfig is excluded
 * via @Profile("!test").
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(final HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            // Add filter to propagate MockMvc authentication to SecurityContextHolder
            .addFilterBefore(testSecurityContextPropagationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Filter that propagates authentication from MockMvc's request context to
     * SecurityContextHolder.
     * This is needed when @AutoConfigureMockMvc(addFilters = false) disables
     * SecurityContextPersistenceFilter.
     * Note: This filter only works if filters are enabled. When addFilters = false,
     * tests should use
     * TestJwtUtils.setSecurityContext() directly instead.
     */
    @Bean
    @Primary
    public OncePerRequestFilter testSecurityContextPropagationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                final HttpServletRequest request,
                final HttpServletResponse response,
                final FilterChain filterChain) throws ServletException, IOException {

                // Check if request has authentication attribute set by MockMvc's
                // .with(user(...))
                // Spring Security test support sets authentication as a request attribute
                org.springframework.security.core.Authentication authentication = null;

                // Check for SecurityContext in request attributes (used by Spring Security test
                // support)
                final Object contextAttr = request.getAttribute(
                    org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                if (contextAttr instanceof org.springframework.security.core.context.SecurityContext) {
                    final org.springframework.security.core.context.SecurityContext context = (org.springframework.security.core.context.SecurityContext) contextAttr;
                    authentication = context.getAuthentication();
                } else {
                    // Fallback: iterate through all request attributes to find Authentication or
                    // SecurityContext
                    final java.util.Enumeration<String> attrNames = request.getAttributeNames();
                    while (attrNames.hasMoreElements() && authentication == null) {
                        final String attrName = attrNames.nextElement();
                        final Object attrValue = request.getAttribute(attrName);
                        if (attrValue instanceof org.springframework.security.core.Authentication) {
                            authentication = (org.springframework.security.core.Authentication) attrValue;
                            break;
                        } else if (attrValue instanceof org.springframework.security.core.context.SecurityContext) {
                            final org.springframework.security.core.context.SecurityContext context = (org.springframework.security.core.context.SecurityContext) attrValue;
                            authentication = context.getAuthentication();
                            break;
                        }
                    }
                }

                // If we found authentication, set it in SecurityContextHolder
                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        // Use same BCrypt strength as production (12) for consistency
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    @Primary
    public JwtTokenProvider jwtTokenProvider() {
        final JwtTokenProvider mock = org.mockito.Mockito.mock(JwtTokenProvider.class);
        org.mockito.Mockito.when(mock.generateToken(
            org.mockito.ArgumentMatchers.any(java.util.UUID.class),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(uk.co.aosd.flash.domain.UserRole.class)))
            .thenReturn("test-jwt-token");
        return mock;
    }

    @Bean
    @Primary
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }

    @Bean
    @Primary
    public JwtAuthenticationFilter jwtAuthenticationFilter(final JwtTokenProvider jwtTokenProvider) {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }

    @Bean
    @Primary
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
