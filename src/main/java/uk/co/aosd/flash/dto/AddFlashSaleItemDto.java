package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

/**
 * DTO for adding a Flash Sale Item to an existing sale.
 */
@Schema(
    name = "AddFlashSaleItem",
    description = "Request body for adding an item to an existing flash sale (admin API)."
)
public record AddFlashSaleItemDto(
    @Schema(description = "Product identifier to add to the sale.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    @NotEmpty(message = "Product ID cannot be empty") String productId,
    @Schema(description = "Stock allocated to this sale item.", example = "25")
    @Min(value = 0, message = "Allocated stock cannot be negative") Integer allocatedStock,
    @Schema(description = "Sale price for this item.", example = "19.99")
    BigDecimal salePrice)
    implements Serializable {
}
