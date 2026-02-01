package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import uk.co.aosd.flash.domain.OrderStatus;

/**
 * DTO for a single order status change in the history.
 */
@Schema(
    name = "OrderStatusHistory",
    description = "A single order status change with timestamp and optional actor."
)
public record OrderStatusHistoryDto(
    @Schema(description = "History entry identifier.")
    UUID id,
    @Schema(description = "Status before the change.")
    OrderStatus fromStatus,
    @Schema(description = "Status after the change.")
    OrderStatus toStatus,
    @Schema(description = "When the change occurred (ISO-8601).")
    OffsetDateTime changedAt,
    @Schema(description = "User who made the change (null for system/background).")
    UUID changedByUserId)
    implements Serializable {
}
