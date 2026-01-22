package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * A DTO for active sales in the client API.
 */
public record ClientActiveSaleDto(
    String saleId,
    String title,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    String productId,
    Integer allocatedStock,
    Integer soldCount,
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
