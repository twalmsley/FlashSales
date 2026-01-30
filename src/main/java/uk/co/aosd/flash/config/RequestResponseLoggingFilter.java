package uk.co.aosd.flash.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Logs API request/response with correlation ID (X-Request-Id in MDC),
 * sanitized headers, and optionally truncated bodies. Applies only to /api/**.
 */
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID = "requestId";

    private final LoggingProperties properties;

    public RequestResponseLoggingFilter(final LoggingProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        if (!Boolean.TRUE.equals(properties.enabled())) {
            return true;
        }
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final FilterChain filterChain) throws ServletException, IOException {

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_REQUEST_ID, requestId);

        try {
            int maxBody = properties.maxBodyLength() != null && properties.maxBodyLength() > 0
                ? properties.maxBodyLength()
                : 1024;
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, maxBody);
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

            long startNanos = System.nanoTime();
            try {
                filterChain.doFilter(wrappedRequest, wrappedResponse);
            } finally {
                wrappedResponse.copyBodyToResponse();
            }
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;

            logRequest(wrappedRequest);
            logResponse(wrappedRequest, wrappedResponse, durationMs);
        } finally {
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    private void logRequest(final ContentCachingRequestWrapper request) {
        if (!log.isInfoEnabled()) {
            return;
        }
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        if (query != null) {
            uri = uri + "?" + query;
        }
        Map<String, String> headers = sanitizedHeaders(request);
        if (properties.isIncludeBody()) {
            String body = getRequestBody(request);
            log.debug("API request {} {} headers={} body={}", method, uri, headers, body);
        } else {
            log.info("API request {} {}", method, uri);
        }
    }

    private void logResponse(
        final ContentCachingRequestWrapper request,
        final ContentCachingResponseWrapper response,
        final long durationMs) {
        if (!log.isInfoEnabled()) {
            return;
        }
        int status = response.getStatus();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        if (properties.isIncludeBody()) {
            String body = getResponseBody(response);
            log.debug("API response {} {} status={} durationMs={} body={}", method, uri, status, durationMs, body);
        } else {
            log.info("API response {} {} status={} durationMs={}", method, uri, status, durationMs);
        }
    }

    private Map<String, String> sanitizedHeaders(final HttpServletRequest request) {
        List<String> sensitive = properties.sensitiveHeaders() != null
            ? properties.sensitiveHeaders()
            : List.of("Authorization", "Cookie");
        Map<String, String> out = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) {
            return Collections.emptyMap();
        }
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name == null) {
                continue;
            }
            boolean sensitiveHeader = sensitive.stream()
                .anyMatch(s -> s.equalsIgnoreCase(name));
            if (sensitiveHeader) {
                out.put(name, "[REDACTED]");
            } else {
                out.put(name, request.getHeader(name));
            }
        }
        return out;
    }

    private String getRequestBody(final ContentCachingRequestWrapper request) {
        if (shouldExcludeBody(request.getRequestURI())) {
            return "[EXCLUDED]";
        }
        byte[] content = request.getContentAsByteArray();
        return truncateBody(content, request.getCharacterEncoding());
    }

    private String getResponseBody(final ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        return truncateBody(content, response.getCharacterEncoding());
    }

    private boolean shouldExcludeBody(final String requestUri) {
        List<String> paths = properties.excludeBodyPaths();
        if (paths == null) {
            return false;
        }
        for (String path : paths) {
            if (requestUri != null && requestUri.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    private String truncateBody(final byte[] content, final String charsetName) {
        if (content == null || content.length == 0) {
            return "";
        }
        int max = properties.maxBodyLength() > 0 ? properties.maxBodyLength() : 1024;
        String charset = charsetName != null && !charsetName.isBlank() ? charsetName : StandardCharsets.UTF_8.name();
        try {
            String s = new String(content, charset);
            if (s.length() > max) {
                return s.substring(0, max) + "...[truncated]";
            }
            return s;
        } catch (Exception e) {
            return "<binary length=" + content.length + ">";
        }
    }
}
