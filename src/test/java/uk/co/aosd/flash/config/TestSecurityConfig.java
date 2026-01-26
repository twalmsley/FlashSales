package uk.co.aosd.flash.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import uk.co.aosd.flash.security.JwtAuthenticationEntryPoint;
import uk.co.aosd.flash.security.JwtAuthenticationFilter;
import uk.co.aosd.flash.services.JwtTokenProvider;

/**
 * Test security configuration that disables security for @WebMvcTest.
 * Provides all security beans needed for tests since SecurityConfig is excluded via @Profile("!test").
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(final HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
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
        return org.mockito.Mockito.mock(JwtTokenProvider.class);
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
}
