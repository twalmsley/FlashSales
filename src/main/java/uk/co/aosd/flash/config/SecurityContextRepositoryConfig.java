package uk.co.aosd.flash.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Provides {@link SecurityContextRepository} as a bean so it can be shared by
 * {@link uk.co.aosd.flash.security.CustomAuthenticationSuccessHandler} and the
 * web security filter chain without creating a circular dependency on
 * {@link SecurityConfig}.
 */
@Configuration
@Profile("!test")
public class SecurityContextRepositoryConfig {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
