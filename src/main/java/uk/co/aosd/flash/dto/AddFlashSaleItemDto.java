package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

/**
 * DTO for adding a Flash Sale Item to an existing sale.
 */
public record AddFlashSaleItemDto(
    @NotEmpty(message = "Product ID cannot be empty") String productId,
    @Min(value = 0, message = "Allocated stock cannot be negative") Integer allocatedStock,
    BigDecimal salePrice)
    implements Serializable {
}
