package uk.co.aosd.flash.errorhandling;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.co.aosd.flash.exc.DuplicateEntityException;
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

/**
 * Global Exception Handling.
 * Provides centralized exception handling for all REST controllers.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ErrorMapper errorMapper;

    /**
     * Handle validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        log.warn("Validation error: {}", e.getMessage());
        final StringBuilder strBuilder = new StringBuilder();

        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName;
            try {
                fieldName = ((FieldError) error).getField();
            } catch (final ClassCastException ex) {
                fieldName = error.getObjectName();
            }
            final String message = error.getDefaultMessage();
            strBuilder.append(String.format("%s: %s\n", fieldName, message));
        });

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
            .body(errorMapper.createErrorMap(strBuilder.toString().trim()));
    }

    /**
     * Handle duplicate entity exceptions.
     */
    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateEntityException(final DuplicateEntityException e) {
        log.warn("Duplicate entity error: id={}, name={}", e.getId(), e.getName());
        final String message = String.format("Entity with id '%s' and name '%s' already exists", e.getId(), e.getName());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(errorMapper.createErrorMap(message));
    }

    /**
     * Handle product not found exceptions.
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleProductNotFoundException(final ProductNotFoundException e) {
        log.warn("Product not found: id={}", e.getId());
        final String message = String.format("Product with id '%s' not found", e.getId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(errorMapper.createErrorMap(message));
    }

    /**
     * Handle invalid sale times exceptions.
     */
    @ExceptionHandler(InvalidSaleTimesException.class)
    public ResponseEntity<Map<String, String>> handleInvalidSaleTimesException(final InvalidSaleTimesException e) {
        log.warn("Invalid sale times: start={}, end={}", e.getStartTime(), e.getEndTime());
        final String message = String.format("Start time must be before end time. Start: %s, End: %s",
            e.getStartTime(), e.getEndTime());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(errorMapper.createErrorMap(message));
    }

    /**
     * Handle sale duration too short exceptions.
     */
    @ExceptionHandler(SaleDurationTooShortException.class)
    public ResponseEntity<Map<String, String>> handleSaleDurationTooShortException(final SaleDurationTooShortException e) {
        log.warn("Sale duration too short: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(errorMapper.createErrorMap(e.getMessage()));
    }

    /**
     * Handle database constraint violation exceptions.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleDatabaseConstraintViolationException(final ConstraintViolationException e) {
        log.error("Database constraint violation: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(errorMapper.createErrorMap("The request violates a data constraint. Please check your input."));
    }

    /**
     * Handle data integrity violation exceptions (database constraint violations).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolationException(final DataIntegrityViolationException e) {
        log.error("Data integrity violation: {}", e.getMessage(), e);
        String message = "The request violates a data constraint. Please check your input.";
        // Check if it's a reserved count constraint violation
        if (e.getMessage() != null && e.getMessage().contains("reserved_count")) {
            message = "Reserved count cannot exceed total physical stock.";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(errorMapper.createErrorMap(message));
    }

    /**
     * Handle illegal argument exceptions (business rule violations).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(final IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(errorMapper.createErrorMap(e.getMessage()));
    }

    /**
     * Handle insufficient resources exceptions.
     */
    @ExceptionHandler(InsufficientResourcesException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientResourcesException(final InsufficientResourcesException e) {
        log.warn("Insufficient resources: id={}", e.getId());
        final String message = String.format("Not enough remaining product stock to reserve for product(s): %s", e.getId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(errorMapper.createErrorMap(message));
    }

    /**
     * Handle malformed JSON or request body parsing errors.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadableException(final HttpMessageNotReadableException e) {
        log.warn("Malformed request body: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(errorMapper.createErrorMap("Malformed request body. Please check your JSON format."));
    }

    /**
     * Handle missing request parameter exceptions.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingServletRequestParameterException(
        final MissingServletRequestParameterException e) {
        log.warn("Missing request parameter: {}", e.getParameterName());
        final String message = String.format("Missing required parameter: %s", e.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(errorMapper.createErrorMap(message));
    }

    /**
     * Handle database access exceptions (connection issues, etc.).
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDataAccessException(final DataAccessException e) {
        log.error("Database access error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorMapper.createErrorMap("A database error occurred. Please try again later."));
    }

    /**
     * Handle sale not active exceptions.
     */
    @ExceptionHandler(SaleNotActiveException.class)
    public ResponseEntity<Map<String, String>> handleSaleNotActiveException(final SaleNotActiveException e) {
        log.warn("Sale not active: saleId={}, endTime={}, currentTime={}", e.getSaleId(), e.getEndTime(), e.getCurrentTime());
        final String message = String.format("Sale has ended. End time: %s, Current time: %s", e.getEndTime(), e.getCurrentTime());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(errorMapper.createErrorMap(message));
    }

    /**
     * Handle insufficient stock exceptions.
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientStockException(final InsufficientStockException e) {
        log.warn("Insufficient stock: flashSaleItemId={}, requested={}, available={}", 
            e.getFlashSaleItemId(), e.getRequestedQuantity(), e.getAvailableStock());
        final String message = String.format("Insufficient stock. Requested: %d, Available: %d", 
            e.getRequestedQuantity(), e.getAvailableStock());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(errorMapper.createErrorMap(message));
    }

    /**
     * Handle order not found exceptions.
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOrderNotFoundException(final OrderNotFoundException e) {
        log.warn("Order not found: orderId={}", e.getOrderId());
        final String message = String.format("Order with id '%s' not found", e.getOrderId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(errorMapper.createErrorMap(message));
    }

    /**
     * Handle flash sale not found exceptions.
     */
    @ExceptionHandler(FlashSaleNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleFlashSaleNotFoundException(final FlashSaleNotFoundException e) {
        log.warn("Flash sale not found: saleId={}", e.getSaleId());
        final String message = String.format("Flash sale with id '%s' not found", e.getSaleId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(errorMapper.createErrorMap(message));
    }

    /**
     * Handle flash sale item not found exceptions.
     */
    @ExceptionHandler(FlashSaleItemNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleFlashSaleItemNotFoundException(final FlashSaleItemNotFoundException e) {
        log.warn("Flash sale item not found: itemId={}", e.getItemId());
        final String message = String.format("Flash sale item with id '%s' not found", e.getItemId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(errorMapper.createErrorMap(message));
    }

    /**
     * Handle invalid order status exceptions.
     */
    @ExceptionHandler(InvalidOrderStatusException.class)
    public ResponseEntity<Map<String, String>> handleInvalidOrderStatusException(final InvalidOrderStatusException e) {
        log.warn("Invalid order status: orderId={}, currentStatus={}, requiredStatus={}, operation={}", 
            e.getOrderId(), e.getCurrentStatus(), e.getRequiredStatus(), e.getOperation());
        final String message = String.format("Invalid order status for operation '%s'. Current: %s, Required: %s", 
            e.getOperation(), e.getCurrentStatus(), e.getRequiredStatus());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(errorMapper.createErrorMap(message));
    }

    /**
     * Handle all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(final Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorMapper.createErrorMap("An unexpected error occurred. Please contact support if the problem persists."));
    }
}
