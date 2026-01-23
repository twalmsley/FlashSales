package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO for creating a new order.
 */
public record CreateOrderDto(
    @NotNull(message = "User ID is required")
    UUID userId,
    
    @NotNull(message = "Flash sale item ID is required")
    UUID flashSaleItemId,
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    Integer quantity)
    implements Serializable {
}
