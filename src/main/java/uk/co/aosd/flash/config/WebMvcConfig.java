package uk.co.aosd.flash.config;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration. Registers formatters and converters for web binding,
 * including support for HTML5 {@code datetime-local} input values
 * ({@code yyyy-MM-dd'T'HH:mm} or {@code yyyy-MM-dd'T'HH:mm:ss}) when binding to
 * {@link OffsetDateTime}.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** Parses HTML5 datetime-local: yyyy-MM-ddTHH:mm or yyyy-MM-ddTHH:mm:ss (no offset). */
    private static final DateTimeFormatter DATETIME_LOCAL = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd'T'HH:mm")
        .optionalStart().appendPattern(":ss").optionalEnd()
        .toFormatter();

    /** Format for HTML5 datetime-local value (no seconds, no offset). */
    private static final DateTimeFormatter DATETIME_LOCAL_OUTPUT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    public void addFormatters(final FormatterRegistry registry) {
        registry.addConverter(new StringToOffsetDateTimeConverter());
        registry.addFormatterForFieldType(OffsetDateTime.class, new OffsetDateTimeFormatter());
    }

    /**
     * Formats {@link OffsetDateTime} for HTML5 datetime-local inputs
     * (yyyy-MM-dd'T'HH:mm in system default zone).
     */
    static class OffsetDateTimeFormatter implements org.springframework.format.Formatter<OffsetDateTime> {

        @Override
        public OffsetDateTime parse(final String text, final Locale locale) {
            return new StringToOffsetDateTimeConverter().convert(text);
        }

        @Override
        public String print(final OffsetDateTime object, final Locale locale) {
            if (object == null) {
                return "";
            }
            return object.atZoneSameInstant(ZoneId.systemDefault()).format(DATETIME_LOCAL_OUTPUT);
        }
    }

    /**
     * Converts strings to {@link OffsetDateTime}. Accepts:
     * <ul>
     *   <li>Full ISO-8601 date-time with offset (e.g. 2026-02-01T09:00:00Z)</li>
     *   <li>HTML5 datetime-local style without offset (e.g. 2026-02-01T09:00 or 2026-02-01T09:00:00),
     *       interpreted in the JVM default zone.</li>
     * </ul>
     */
    static class StringToOffsetDateTimeConverter implements Converter<String, OffsetDateTime> {

        @Override
        public OffsetDateTime convert(final String source) {
            if (source == null || source.isBlank()) {
                return null;
            }
            final String trimmed = source.trim();
            try {
                return OffsetDateTime.parse(trimmed);
            } catch (final DateTimeParseException ignored) {
                // Try datetime-local style (no offset)
            }
            try {
                final LocalDateTime local = LocalDateTime.parse(trimmed, DATETIME_LOCAL);
                return local.atZone(ZoneId.systemDefault()).toOffsetDateTime();
            } catch (final DateTimeParseException e) {
                throw new IllegalArgumentException("Cannot parse date-time: " + source, e);
            }
        }
    }
}
