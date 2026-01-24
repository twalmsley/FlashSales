package uk.co.aosd.flash.dto;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard error response shape returned by the API.
 *
 * <p>At runtime, errors are currently produced via {@code GlobalExceptionHandler} as a JSON object
 * with a single {@code message} field. This DTO exists primarily to provide a stable, reusable
 * schema in OpenAPI documentation.</p>
 */
@Schema(
    name = "ErrorResponse",
    description = "Standard error response returned when a request cannot be processed."
)
public record ErrorResponseDto(
    @Schema(
        description = "Human-readable error message.",
        example = "Product with id 'product-123' not found"
    )
    String message
) implements Serializable {
}

