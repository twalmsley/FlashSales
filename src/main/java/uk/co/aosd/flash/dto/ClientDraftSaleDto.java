package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A DTO for draft sales in the client API.
 */
@Schema(
    name = "ClientDraftSale",
    description = "Client-facing view of a draft (upcoming) flash sale."
)
public record ClientDraftSaleDto(
    @Schema(description = "Flash sale identifier.", example = "5b3c3f18-2f88-4c38-8b35-9aa6d9b9f5af")
    String saleId,
    @Schema(description = "Sale title.", example = "Winter Deals")
    String title,
    @Schema(description = "Sale start time (ISO-8601).", example = "2026-02-01T09:00:00Z")
    OffsetDateTime startTime,
    @Schema(description = "Sale end time (ISO-8601).", example = "2026-02-01T12:00:00Z")
    OffsetDateTime endTime,
    @Schema(description = "Products included in this upcoming sale.")
    List<DraftSaleProductDto> products)
    implements Serializable {

    /**
     * A DTO for a product in a draft sale.
     */
    @Schema(
        name = "DraftSaleProduct",
        description = "Product allocation for an upcoming (draft) sale."
    )
    public record DraftSaleProductDto(
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
        @Schema(description = "Sale price for this item.", example = "19.99")
        BigDecimal salePrice)
        implements Serializable {
    }
}
