package uk.co.aosd.flash.dto;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for updating product stock.
 */
@Schema(
    name = "UpdateProductStock",
    description = "Request body for updating a product's total physical stock."
)
public record UpdateProductStockDto(
    @Schema(description = "New total physical stock value.", example = "250")
    @NotNull(message = "totalPhysicalStock is required")
    @Min(value = 0, message = "Stock cannot be negative") Integer totalPhysicalStock
) implements Serializable {
}

