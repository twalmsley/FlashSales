package uk.co.aosd.flash.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Registers API request/response logging filter and its configuration.
 * Filter is applied only to /api/** when enabled via app.logging.api.enabled.
 * Disables servlet-container registration so the filter runs only in the Security chain
 * (avoids NullPointerException from duplicate registration).
 */
@Configuration
@EnableConfigurationProperties(LoggingProperties.class)
@Profile("!test")
public class RequestLoggingConfig {

    @Bean
    public RequestResponseLoggingFilter requestResponseLoggingFilter(final LoggingProperties properties) {
        return new RequestResponseLoggingFilter(properties);
    }

    @Bean
    public FilterRegistrationBean<RequestResponseLoggingFilter> requestResponseLoggingFilterRegistration(
        final RequestResponseLoggingFilter filter) {
        FilterRegistrationBean<RequestResponseLoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
