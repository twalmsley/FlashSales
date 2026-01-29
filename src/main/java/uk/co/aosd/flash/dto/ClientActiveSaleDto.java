package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A DTO for active sales in the client API.
 */
@Schema(
    name = "ClientActiveSale",
    description = "Client-facing view of an active flash sale item that still has remaining stock."
)
public record ClientActiveSaleDto(
    @Schema(description = "Flash sale identifier.", example = "5b3c3f18-2f88-4c38-8b35-9aa6d9b9f5af")
    String saleId,
    @Schema(description = "Flash sale item identifier.", example = "b1b7a3c0-8d3b-4d10-8cc1-3c5f88f4bb5a")
    String flashSaleItemId,
    @Schema(description = "Sale title.", example = "Winter Deals")
    String title,
    @Schema(description = "Sale start time (ISO-8601).", example = "2026-02-01T09:00:00Z")
    OffsetDateTime startTime,
    @Schema(description = "Sale end time (ISO-8601).", example = "2026-02-01T12:00:00Z")
    OffsetDateTime endTime,
    @Schema(description = "Product identifier.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    String productId,
    @Schema(description = "Product name.", example = "Winter Jacket")
    String productName,
    @Schema(description = "Product description.", example = "Warm winter jacket.")
    String productDescription,
    @Schema(description = "Product base price (catalog price).", example = "49.99")
    BigDecimal basePrice,
    @Schema(description = "Stock allocated to this sale item.", example = "25")
    Integer allocatedStock,
    @Schema(description = "Quantity already sold.", example = "3")
    Integer soldCount,
    @Schema(description = "Sale price for this item.", example = "19.99")
    BigDecimal salePrice)
    implements Serializable {

    /**
     * Computes the remaining time for the sale based on the provided time parameter.
     * 
     * @param currentTime the current time to compare against the sale's end time
     * @return the remaining duration until the sale ends, or Duration.ZERO if the sale has already ended
     */
    public Duration getRemainingTime(OffsetDateTime currentTime) {
        Duration remaining = Duration.between(currentTime, endTime);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
