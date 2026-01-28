package uk.co.aosd.flash.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import uk.co.aosd.flash.security.CustomAuthenticationSuccessHandler;
import uk.co.aosd.flash.security.JwtAuthenticationEntryPoint;
import uk.co.aosd.flash.security.JwtAuthenticationFilter;
import uk.co.aosd.flash.services.JwtTokenProvider;

/**
 * Spring Security configuration supporting both JWT-based authentication (for
 * API)
 * and form-based authentication (for UI).
 * Follows Spring Boot 4 best practices with component-based configuration.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Profile("!test")
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;

    /**
     * Security filter chain for API endpoints (JWT-based, stateless).
     * This chain handles all /api/** requests.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(final HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            // Disable CSRF for stateless JWT-based API
            .csrf(AbstractHttpConfigurer::disable)
            // Configure stateless session management
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Configure security headers
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentTypeOptions(contentTypeOptions -> {
                })
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)))
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public API endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Admin API endpoints require ADMIN_USER role
                .requestMatchers("/api/v1/admin/**", "/api/v1/products/**").hasRole("ADMIN_USER")
                // All other API endpoints require authentication
                .anyRequest().authenticated())
            // Configure exception handling
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint()))
            // Add JWT filter before username/password authentication filter
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Security filter chain for UI endpoints (form-based, stateful).
     * This chain handles all non-API requests.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(
        final HttpSecurity http,
        final SecurityContextRepository securityContextRepository) throws Exception {
        http
            .securityMatcher("/**")
            // Use explicit SecurityContextRepository so the login success handler can save
            // the context the same way the filter loads it (fixes admin links not showing
            // after redirect).
            .securityContext(sec -> sec.securityContextRepository(securityContextRepository))
            // Enable CSRF for form submissions
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**") // API endpoints are handled by apiSecurityFilterChain
            )
            // Configure stateful session management for UI.
            // Use sessionFixation().none() so the SecurityContext we save in the login
            // success
            // handler is stored in the same session the client uses on the redirect (GET
            // /).
            // changeSessionId() can cause the redirect to see an empty session and hide
            // admin links.
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation().none())
            // Configure security headers
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentTypeOptions(contentTypeOptions -> {
                })
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)))
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public UI pages
                .requestMatchers("/", "/login", "/register", "/sales", "/sales/**", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                // Health endpoint remains public for Kubernetes liveness/readiness probes
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                // All other actuator endpoints require ADMIN_USER role for security
                .requestMatchers("/actuator/**").hasRole("ADMIN_USER")
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Admin UI pages require ADMIN_USER role
                .requestMatchers("/admin/**").hasRole("ADMIN_USER")
                // All other UI pages require authentication
                .anyRequest().authenticated())
            // Configure form login
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler)
                .failureUrl("/login?error=true")
                .permitAll())
            // Configure logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll())
            // Configure exception handling
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/403"));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use BCrypt with strength 12 (recommended for Spring Boot 4)
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(final AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }
}
