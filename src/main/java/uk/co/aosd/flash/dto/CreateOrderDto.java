package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO for creating a new order.
 */
@Schema(
    name = "CreateOrder",
    description = "Request body for placing an order against an active flash sale item. User ID is extracted from JWT token."
)
public record CreateOrderDto(
    @Schema(
        description = "Flash sale item identifier being purchased.",
        example = "b1b7a3c0-8d3b-4d10-8cc1-3c5f88f4bb5a"
    )
    @NotNull(message = "Flash sale item ID is required")
    UUID flashSaleItemId,
    
    @Schema(description = "Quantity requested.", example = "2")
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    Integer quantity)
    implements Serializable {
}
