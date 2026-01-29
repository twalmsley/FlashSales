package uk.co.aosd.flash.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for CORS (Cross-Origin Resource Sharing).
 * Binds to {@code app.cors.*} in application configuration.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
    List<String> allowedOrigins,
    List<String> allowedMethods,
    List<String> allowedHeaders,
    boolean allowCredentials,
    long maxAge
) {
}
