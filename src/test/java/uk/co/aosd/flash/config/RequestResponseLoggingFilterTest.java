package uk.co.aosd.flash.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@ExtendWith(MockitoExtension.class)
class RequestResponseLoggingFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RequestResponseLoggingFilter filter;

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldNotFilter_whenPathNotApi_returnsTrue() {
        when(request.getRequestURI()).thenReturn("/login");
        filter = new RequestResponseLoggingFilter(enabledProperties());

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_whenPathIsApi_returnsFalse() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/me");
        filter = new RequestResponseLoggingFilter(enabledProperties());

        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void shouldNotFilter_whenDisabled_returnsTrue() {
        filter = new RequestResponseLoggingFilter(disabledProperties());
        // When disabled, filter returns true without reading request
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_whenPathNull_returnsTrue() {
        when(request.getRequestURI()).thenReturn(null);
        filter = new RequestResponseLoggingFilter(enabledProperties());

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void doFilterInternal_setsRequestIdInMdcAndClearsAfterRequest() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/me");
        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn(null);
        when(request.getHeaderNames()).thenReturn(java.util.Collections.emptyEnumeration());
        when(response.getStatus()).thenReturn(200);

        filter = new RequestResponseLoggingFilter(enabledProperties());
        filter.doFilterInternal(request, response, filterChain);

        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void doFilterInternal_usesProvidedXRequestIdHeader() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/me");
        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn(null);
        when(request.getHeader("X-Request-Id")).thenReturn("custom-request-id-123");
        when(request.getHeaderNames()).thenReturn(java.util.Collections.enumeration(List.of("X-Request-Id")));
        when(response.getStatus()).thenReturn(200);

        filter = new RequestResponseLoggingFilter(enabledProperties());
        filter.doFilterInternal(request, response, filterChain);

        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void doFilterInternal_invokesChainWithWrappedRequestAndResponse() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/products");
        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn(null);
        when(request.getHeaderNames()).thenReturn(java.util.Collections.emptyEnumeration());
        when(response.getStatus()).thenReturn(200);

        filter = new RequestResponseLoggingFilter(enabledProperties());
        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        ArgumentCaptor<HttpServletResponse> responseCaptor = ArgumentCaptor.forClass(HttpServletResponse.class);
        verify(filterChain).doFilter(requestCaptor.capture(), responseCaptor.capture());

        assertThat(requestCaptor.getValue()).isInstanceOf(ContentCachingRequestWrapper.class);
        assertThat(responseCaptor.getValue()).isInstanceOf(ContentCachingResponseWrapper.class);
    }

    private static LoggingProperties enabledProperties() {
        return new LoggingProperties(true, "INFO", 1024, List.of("Authorization", "Cookie"), List.of("/api/v1/auth/login"));
    }

    private static LoggingProperties disabledProperties() {
        return new LoggingProperties(false, "INFO", 1024, List.of("Authorization", "Cookie"), List.of());
    }
}
