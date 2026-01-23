package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for a Flash Sale Item in responses.
 */
public record FlashSaleItemDto(
    String id,
    String productId,
    String productName,
    Integer allocatedStock,
    Integer soldCount,
    BigDecimal salePrice)
    implements Serializable {
}
