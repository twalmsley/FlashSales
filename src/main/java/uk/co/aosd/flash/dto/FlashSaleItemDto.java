package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for a Flash Sale Item in responses.
 */
@Schema(
    name = "FlashSaleItem",
    description = "An individual item within a flash sale."
)
public record FlashSaleItemDto(
    @Schema(description = "Flash sale item identifier.", example = "b1b7a3c0-8d3b-4d10-8cc1-3c5f88f4bb5a")
    String id,
    @Schema(description = "Product identifier.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    String productId,
    @Schema(description = "Product name.", example = "Wireless Mouse")
    String productName,
    @Schema(description = "Stock allocated to this sale item.", example = "25")
    Integer allocatedStock,
    @Schema(description = "Quantity already sold.", example = "3")
    Integer soldCount,
    @Schema(description = "Sale price for this item.", example = "19.99")
    BigDecimal salePrice)
    implements Serializable {
}
