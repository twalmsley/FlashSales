package uk.co.aosd.flash.dto;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import uk.co.aosd.flash.domain.OrderStatus;

/**
 * DTO for updating order status (admin operation).
 */
@Schema(
    name = "UpdateOrderStatus",
    description = "Request body for updating an order status (admin operation)."
)
public record UpdateOrderStatusDto(
    @Schema(
        description = "New order status.",
        example = "PAID"
    )
    @NotNull(message = "Status is required")
    OrderStatus status)
    implements Serializable {
}
