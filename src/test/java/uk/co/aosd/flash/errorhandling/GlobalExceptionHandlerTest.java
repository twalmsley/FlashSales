package uk.co.aosd.flash.errorhandling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import uk.co.aosd.flash.exc.DuplicateEntityException;
import uk.co.aosd.flash.exc.InsufficientResourcesException;
import uk.co.aosd.flash.exc.InvalidSaleTimesException;
import uk.co.aosd.flash.exc.ProductNotFoundException;
import uk.co.aosd.flash.exc.SaleDurationTooShortException;

/**
 * Tests for GlobalExceptionHandler.
 */
public class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private ErrorMapper errorMapper;

    @BeforeEach
    public void setUp() {
        errorMapper = new ErrorMapper();
        handler = new GlobalExceptionHandler(errorMapper);
    }

    @Test
    public void shouldHandleDuplicateEntityException() {
        final DuplicateEntityException ex = new DuplicateEntityException("test-id", "Test Product");
        final ResponseEntity<Map<String, String>> response = handler.handleDuplicateEntityException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").contains("test-id"));
        assertTrue(response.getBody().get("message").contains("Test Product"));
    }

    @Test
    public void shouldHandleProductNotFoundException() {
        final ProductNotFoundException ex = new ProductNotFoundException("product-123");
        final ResponseEntity<Map<String, String>> response = handler.handleProductNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").contains("product-123"));
    }

    @Test
    public void shouldHandleInvalidSaleTimesException() {
        final OffsetDateTime start = OffsetDateTime.of(2026, 1, 1, 14, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime end = OffsetDateTime.of(2026, 1, 1, 13, 0, 0, 0, ZoneOffset.UTC);
        final InvalidSaleTimesException ex = new InvalidSaleTimesException(start, end);
        final ResponseEntity<Map<String, String>> response = handler.handleInvalidSaleTimesException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").contains("Start time must be before end time"));
    }

    @Test
    public void shouldHandleSaleDurationTooShortException() {
        final SaleDurationTooShortException ex = new SaleDurationTooShortException("Duration too short", 5.0f, 10.0f);
        final ResponseEntity<Map<String, String>> response = handler.handleSaleDurationTooShortException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertEquals("Duration too short", response.getBody().get("message"));
    }

    @Test
    public void shouldHandleSaleDurationTooShortExceptionWithBackwardCompatibility() {
        final SaleDurationTooShortException ex = new SaleDurationTooShortException("Duration too short");
        final ResponseEntity<Map<String, String>> response = handler.handleSaleDurationTooShortException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertEquals("Duration too short", response.getBody().get("message"));
    }

    @Test
    public void shouldHandleInsufficientResourcesException() {
        final InsufficientResourcesException ex = new InsufficientResourcesException("product-123");
        final ResponseEntity<Map<String, String>> response = handler.handleInsufficientResourcesException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").contains("product-123"));
    }

    @Test
    public void shouldHandleConstraintViolationException() {
        final ConstraintViolationException ex = new ConstraintViolationException("Constraint violated", null, null);
        final ResponseEntity<Map<String, String>> response = handler.handleDatabaseConstraintViolationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").contains("data constraint"));
    }

    @Test
    public void shouldHandleHttpMessageNotReadableException() {
        final HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Malformed JSON", null, null);
        final ResponseEntity<Map<String, String>> response = handler.handleHttpMessageNotReadableException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").contains("Malformed request body"));
    }

    @Test
    public void shouldHandleMissingServletRequestParameterException() {
        final MissingServletRequestParameterException ex = new MissingServletRequestParameterException("paramName", "String");
        final ResponseEntity<Map<String, String>> response = handler.handleMissingServletRequestParameterException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").contains("paramName"));
    }

    @Test
    public void shouldHandleDataAccessException() {
        final DataAccessException ex = new DataAccessException("Database error") {
        };
        final ResponseEntity<Map<String, String>> response = handler.handleDataAccessException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").contains("database error"));
    }

    @Test
    public void shouldHandleGenericException() {
        final RuntimeException ex = new RuntimeException("Unexpected error");
        final ResponseEntity<Map<String, String>> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").contains("unexpected error"));
    }
}
