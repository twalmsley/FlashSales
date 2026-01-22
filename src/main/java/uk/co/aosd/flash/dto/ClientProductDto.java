package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

/**
 * A client view of a product.
 */
public record ClientProductDto(
    String id,
    @NotEmpty(message = "A name must be provided.") String name,
    @NotEmpty(message = "A description must be provided.") String description,
    @Min(value = 0, message = "Price cannot be negative") BigDecimal basePrice)
    implements Serializable {
}
