package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for updating a Flash Sale.
 * All fields are optional.
 */
@Schema(
    name = "UpdateFlashSale",
    description = "Request body for updating flash sale metadata. All fields are optional."
)
public record UpdateFlashSaleDto(
    @Schema(description = "New sale title.", example = "Winter Deals (extended)")
    String title,
    @Schema(description = "Updated start time (ISO-8601).", example = "2026-02-01T09:30:00Z")
    OffsetDateTime startTime,
    @Schema(description = "Updated end time (ISO-8601).", example = "2026-02-01T12:30:00Z")
    OffsetDateTime endTime)
    implements Serializable {
}
