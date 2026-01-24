package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import uk.co.aosd.flash.domain.OrderStatus;

/**
 * DTO for order response.
 */
@Schema(
    name = "OrderResponse",
    description = "Response returned after creating an order or performing an order action (e.g. refund)."
)
public record OrderResponseDto(
    @Schema(description = "Order identifier.", example = "2b8efb9f-6f89-4b2d-8c73-4b2f9d4d2e1a")
    UUID orderId,
    @Schema(description = "Current order status.")
    OrderStatus status,
    @Schema(description = "Additional human-readable message.", example = "Refund processed successfully")
    String message)
    implements Serializable {
}
