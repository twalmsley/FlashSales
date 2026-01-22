package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;
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
}
