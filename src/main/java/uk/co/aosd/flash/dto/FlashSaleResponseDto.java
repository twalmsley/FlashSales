package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import uk.co.aosd.flash.domain.SaleStatus;

/**
 * DTO for Flash Sale responses.
 */
@Schema(
    name = "FlashSale",
    description = "Flash sale returned by the admin API."
)
public record FlashSaleResponseDto(
    @Schema(description = "Flash sale identifier.", example = "5b3c3f18-2f88-4c38-8b35-9aa6d9b9f5af")
    String id,
    @Schema(description = "Sale title.", example = "Winter Deals")
    String title,
    @Schema(description = "Sale start time (ISO-8601).", example = "2026-02-01T09:00:00Z")
    OffsetDateTime startTime,
    @Schema(description = "Sale end time (ISO-8601).", example = "2026-02-01T12:00:00Z")
    OffsetDateTime endTime,
    @Schema(description = "Current sale status.", example = "DRAFT")
    SaleStatus status,
    @Schema(description = "Items included in this sale.")
    List<FlashSaleItemDto> items)
    implements Serializable {
}
