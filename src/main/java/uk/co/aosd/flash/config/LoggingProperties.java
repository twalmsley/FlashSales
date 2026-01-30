package uk.co.aosd.flash.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for API request/response logging.
 * Binds to {@code app.logging.api.*} in application configuration.
 */
@ConfigurationProperties(prefix = "app.logging.api")
public record LoggingProperties(
    Boolean enabled,
    String level,
    Integer maxBodyLength,
    List<String> sensitiveHeaders,
    List<String> excludeBodyPaths
) {
    /**
     * Compact constructor: apply defaults when properties are not set.
     */
    public LoggingProperties {
        if (enabled == null) {
            enabled = true;
        }
        if (level == null) {
            level = "INFO";
        }
        if (maxBodyLength == null) {
            maxBodyLength = 1024;
        }
        if (sensitiveHeaders == null) {
            sensitiveHeaders = List.of("Authorization", "Cookie");
        }
        if (excludeBodyPaths == null) {
            excludeBodyPaths = List.of("/api/v1/auth/login", "/api/v1/auth/register");
        }
    }

    public boolean isIncludeBody() {
        return "DEBUG".equalsIgnoreCase(level);
    }
}
