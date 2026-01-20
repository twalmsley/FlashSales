package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

/**
 * A Product DTO.
 */
public record ProductDto(
    String id,
    @NotEmpty(message = "A name must be provided.") String name,
    @NotEmpty(message = "A description must be provided.") String description,
    @Min(value = 0, message = "Stock cannot be negative") Integer totalPhysicalStock,
    @Min(value = 0, message = "Price cannot be negative") BigDecimal basePrice,
    @Min(value = 0, message = "ReservedCount cannot be negative") Integer reservedCount)
    implements Serializable {
}
