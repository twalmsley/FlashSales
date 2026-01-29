package uk.co.aosd.flash.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration for the API. Registers a {@link CorsConfigurationSource}
 * that applies to {@code /api/**} so cross-origin frontends can call the API.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
@Profile("!test")
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(final CorsProperties properties) {
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.allowedOrigins());
        config.setAllowedMethods(properties.allowedMethods());
        config.setAllowedHeaders(properties.allowedHeaders());
        config.setAllowCredentials(properties.allowCredentials());
        config.setMaxAge(properties.maxAge());

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
