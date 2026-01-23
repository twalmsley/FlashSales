package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

/**
 * DTO for updating a Flash Sale Item.
 * At least one field must be provided.
 */
public record UpdateFlashSaleItemDto(
    @Min(value = 0, message = "Allocated stock cannot be negative") Integer allocatedStock,
    @Positive(message = "Sale price must be positive") BigDecimal salePrice)
    implements Serializable {
    
    /**
     * Validate that at least one field is provided.
     */
    public UpdateFlashSaleItemDto {
        if (allocatedStock == null && salePrice == null) {
            throw new IllegalArgumentException("At least one field (allocatedStock or salePrice) must be provided");
        }
    }
}
