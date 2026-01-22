package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * A DTO for draft sales in the client API.
 */
public record ClientDraftSaleDto(
    String saleId,
    String title,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    List<DraftSaleProductDto> products)
    implements Serializable {

    /**
     * A DTO for a product in a draft sale.
     */
    public record DraftSaleProductDto(
        String productId,
        Integer allocatedStock,
        BigDecimal salePrice)
        implements Serializable {
    }
}
