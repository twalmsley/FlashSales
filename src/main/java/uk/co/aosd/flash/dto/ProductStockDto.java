package uk.co.aosd.flash.dto;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

/**
 * DTO for product stock details.
 */
@Schema(
    name = "ProductStock",
    description = "Stock details for a product (physical, reserved, and available)."
)
public record ProductStockDto(
    @Schema(description = "Product identifier.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    @NotEmpty(message = "ID cannot be empty") String id,
    @Schema(description = "Total physical stock available for this product.", example = "100")
    @Min(value = 0, message = "Stock cannot be negative") Integer totalPhysicalStock,
    @Schema(description = "Quantity currently reserved for upcoming/active sales.", example = "10")
    @Min(value = 0, message = "ReservedCount cannot be negative") Integer reservedCount,
    @Schema(description = "Available stock (totalPhysicalStock - reservedCount).", example = "90")
    @Min(value = 0, message = "AvailableStock cannot be negative") Integer availableStock
) implements Serializable {
}

