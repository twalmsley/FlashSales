package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for refunding an order.
 */
public record RefundOrderDto(
    @NotNull(message = "Order ID is required")
    UUID orderId)
    implements Serializable {
}
