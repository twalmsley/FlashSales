package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for refunding an order.
 */
@Schema(
    name = "RefundOrder",
    description = "Request body for refunding an order."
)
public record RefundOrderDto(
    @Schema(description = "Order identifier to refund.", example = "2b8efb9f-6f89-4b2d-8c73-4b2f9d4d2e1a")
    @NotNull(message = "Order ID is required")
    UUID orderId)
    implements Serializable {
}
