package uk.co.aosd.flash.errorhandling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.core.MethodParameter;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import uk.co.aosd.flash.exc.DuplicateEntityException;
import uk.co.aosd.flash.exc.InvalidCurrentPasswordException;
import uk.co.aosd.flash.exc.FlashSaleItemNotFoundException;
import uk.co.aosd.flash.exc.FlashSaleNotFoundException;
import uk.co.aosd.flash.exc.InsufficientResourcesException;
import uk.co.aosd.flash.exc.InsufficientStockException;
import uk.co.aosd.flash.exc.InvalidOrderStatusException;
import uk.co.aosd.flash.exc.InvalidSaleTimesException;
import uk.co.aosd.flash.exc.OrderNotFoundException;
import uk.co.aosd.flash.exc.ProductNotFoundException;
import uk.co.aosd.flash.exc.SaleDurationTooShortException;
import uk.co.aosd.flash.exc.SaleNotActiveException;
import uk.co.aosd.flash.domain.OrderStatus;

/**
 * Tests for GlobalExceptionHandler.
 */
public class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private ErrorMapper errorMapper;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    public void setUp() {
        errorMapper = new ErrorMapper();
        meterRegistry = new SimpleMeterRegistry();
        handler = new GlobalExceptionHandler(errorMapper);
        handler.setMeterRegistry(meterRegistry);
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
        assertEquals(1, meterRegistry.get("flash.errors").tag("exception", "DuplicateEntityException").tag("status", "409").counter().count());
    }

    @Test
    public void shouldHandleDuplicateEntityException_username_returnsFriendlyMessage() {
        final DuplicateEntityException ex = new DuplicateEntityException("username", "johndoe");
        final ResponseEntity<Map<String, String>> response = handler.handleDuplicateEntityException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Username already taken", response.getBody().get("message"));
    }

    @Test
    public void shouldHandleDuplicateEntityException_email_returnsFriendlyMessage() {
        final DuplicateEntityException ex = new DuplicateEntityException("email", "john@example.com");
        final ResponseEntity<Map<String, String>> response = handler.handleDuplicateEntityException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Email already in use", response.getBody().get("message"));
    }

    @Test
    public void shouldHandleInvalidCurrentPasswordException() {
        final InvalidCurrentPasswordException ex = new InvalidCurrentPasswordException();
        final ResponseEntity<Map<String, String>> response = handler.handleInvalidCurrentPasswordException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").contains("Current password"));
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

    @Test
    public void shouldHandleNoResourceFoundException() {
        final NoResourceFoundException ex = new NoResourceFoundException(
            HttpMethod.GET,
            "favicon.ico",
            "No static resource favicon.ico");
        final ResponseEntity<Map<String, String>> response = handler.handleNoResourceFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").contains("Resource not found"));
    }

    @Test
    public void shouldHandleMethodArgumentNotValidException_withFieldError() throws Exception {
        final MethodParameter parameter = new MethodParameter(
            GlobalExceptionHandlerTest.class.getMethod("dummyMethodForParameter", String.class), 0);
        final BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "username", "must not be blank"));
        final MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        final ResponseEntity<Map<String, String>> response = handler.handleMethodArgumentNotValidException(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").contains("username"));
        assertTrue(response.getBody().get("message").contains("must not be blank"));
    }

    @Test
    public void shouldHandleMethodArgumentNotValidException_withObjectError() throws Exception {
        final MethodParameter parameter = new MethodParameter(
            GlobalExceptionHandlerTest.class.getMethod("dummyMethodForParameter", String.class), 0);
        final BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new ObjectError("target", "Invalid target object"));
        final MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        final ResponseEntity<Map<String, String>> response = handler.handleMethodArgumentNotValidException(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").contains("target"));
        assertTrue(response.getBody().get("message").contains("Invalid target object"));
    }

    @Test
    public void shouldHandleDataIntegrityViolationException_genericMessage() {
        final DataIntegrityViolationException ex = new DataIntegrityViolationException("constraint violation");

        final ResponseEntity<Map<String, String>> response = handler.handleDataIntegrityViolationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").contains("data constraint"));
    }

    @Test
    public void shouldHandleDataIntegrityViolationException_reservedCountMessage() {
        final DataIntegrityViolationException ex = new DataIntegrityViolationException(
            "constraint reserved_count check failed");

        final ResponseEntity<Map<String, String>> response = handler.handleDataIntegrityViolationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("message").contains("Reserved count cannot exceed total physical stock"));
    }

    @Test
    public void shouldHandleIllegalArgumentException() {
        final IllegalArgumentException ex = new IllegalArgumentException("Invalid value");

        final ResponseEntity<Map<String, String>> response = handler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid value", response.getBody().get("message"));
    }

    @Test
    public void shouldHandleSaleNotActiveException() {
        final var endTime = OffsetDateTime.now(ZoneOffset.UTC);
        final var currentTime = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(1);
        final SaleNotActiveException ex = new SaleNotActiveException(UUID.randomUUID(), endTime, currentTime);

        final ResponseEntity<Map<String, String>> response = handler.handleSaleNotActiveException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("message").contains("Sale has ended"));
    }

    @Test
    public void shouldHandleInsufficientStockException() {
        final InsufficientStockException ex = new InsufficientStockException(
            UUID.randomUUID(), 5, 2);

        final ResponseEntity<Map<String, String>> response = handler.handleInsufficientStockException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("message").contains("Insufficient stock"));
        assertTrue(response.getBody().get("message").contains("5"));
        assertTrue(response.getBody().get("message").contains("2"));
    }

    @Test
    public void shouldHandleOrderNotFoundException() {
        final var orderId = UUID.randomUUID();
        final OrderNotFoundException ex = new OrderNotFoundException(orderId);

        final ResponseEntity<Map<String, String>> response = handler.handleOrderNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("message").contains(orderId.toString()));
    }

    @Test
    public void shouldHandleFlashSaleNotFoundException() {
        final var saleId = UUID.randomUUID();
        final FlashSaleNotFoundException ex = new FlashSaleNotFoundException(saleId);

        final ResponseEntity<Map<String, String>> response = handler.handleFlashSaleNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("message").contains(saleId.toString()));
    }

    @Test
    public void shouldHandleFlashSaleItemNotFoundException() {
        final var itemId = UUID.randomUUID();
        final FlashSaleItemNotFoundException ex = new FlashSaleItemNotFoundException(itemId);

        final ResponseEntity<Map<String, String>> response = handler.handleFlashSaleItemNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("message").contains(itemId.toString()));
    }

    @Test
    public void shouldHandleInvalidOrderStatusException() {
        final var orderId = UUID.randomUUID();
        final InvalidOrderStatusException ex = new InvalidOrderStatusException(
            orderId, OrderStatus.PENDING, OrderStatus.PAID, "dispatch");

        final ResponseEntity<Map<String, String>> response = handler.handleInvalidOrderStatusException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("message").contains("Invalid order status"));
        assertTrue(response.getBody().get("message").contains("PENDING"));
        assertTrue(response.getBody().get("message").contains("PAID"));
    }

    @Test
    public void shouldHandleBadCredentialsException() {
        final BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        final ResponseEntity<Map<String, String>> response = handler.handleBadCredentialsException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid credentials", response.getBody().get("message"));
    }

    @Test
    public void shouldHandleIllegalStateException_notAuthenticated_returns401() {
        final IllegalStateException ex = new IllegalStateException("User is not authenticated");

        final ResponseEntity<Map<String, String>> response = handler.handleIllegalStateException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Authentication required", response.getBody().get("message"));
    }

    @Test
    public void shouldHandleIllegalStateException_generic_returns500() {
        final IllegalStateException ex = new IllegalStateException("Invalid state");

        final ResponseEntity<Map<String, String>> response = handler.handleIllegalStateException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("message").contains("unexpected error"));
    }

    @SuppressWarnings("unused")
    public static void dummyMethodForParameter(final String arg) {
        // Used only to obtain a MethodParameter for MethodArgumentNotValidException tests
    }
}
