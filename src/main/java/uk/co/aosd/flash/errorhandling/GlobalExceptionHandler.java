package uk.co.aosd.flash.errorhandling;

import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.co.aosd.flash.exc.*;

/**
 * Global Exception Handling.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final ErrorMapper errorMapper;

    /**
     * Handle an Exception.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
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

        return new ResponseEntity<>(errorMapper.createErrorMap(strBuilder.toString()), HttpStatus.UNPROCESSABLE_CONTENT);
    }

    /**
     * Handle an Exception.
     */
    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<String> handleDuplicateProductException(final DuplicateEntityException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getId());
    }

    /**
     * Handle an Exception.
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<String> handleProductNotFoundException(final ProductNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getId());
    }

    /**
     * Handle an Exception.
     */
    @ExceptionHandler(InvalidSaleTimesException.class)
    public ResponseEntity<String> handleInvalidSaleTimesException(final InvalidSaleTimesException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Start should be before end. Start: " + e.getStartTime() + ", End: " + e.getEndTime());
    }

    /**
     * Handle an Exception.
     */
    @ExceptionHandler(SaleDurationTooShortException.class)
    public ResponseEntity<String> handleSaleDurationTooShortException(final SaleDurationTooShortException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    /**
     * Handle an Exception.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleDatabaseConstraintViolationException(final ConstraintViolationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The request violates a data constraint: " + e.getMessage());
    }

    /**
     * Handle an Exception.
     */
    @ExceptionHandler(InsufficientResourcesException.class)
    public ResponseEntity<String> handleInsufficientResourcesException(final InsufficientResourcesException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not enough remaining product stock to reserve: " + e.getId());
    }
}
