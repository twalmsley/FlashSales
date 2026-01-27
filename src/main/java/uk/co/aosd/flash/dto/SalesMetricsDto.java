package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for sales performance metrics.
 */
@Schema(name = "SalesMetrics", description = "Sales performance metrics for flash sales.")
public record SalesMetricsDto(
    @Schema(description = "Total number of flash sales.", example = "25") Long totalSales,
    @Schema(description = "Number of sales by status.") SalesByStatus salesByStatus,
    @Schema(description = "Total items sold across all sales.", example = "1500") Long totalItemsSold,
    @Schema(description = "Average items sold per sale.", example = "60.0") BigDecimal averageItemsSoldPerSale,
    @Schema(description = "Total items allocated across all sales.", example = "2000") Long totalItemsAllocated,
    @Schema(description = "Sales conversion rate (items sold / items allocated).", example = "0.75") BigDecimal conversionRate,
    @Schema(description = "Top performing sales by items sold.") List<TopSale> topSalesByItemsSold)
    implements Serializable {

    @Schema(description = "Sales count by status.")
    public record SalesByStatus(
        @Schema(description = "Number of DRAFT sales.", example = "5") Long draft,
        @Schema(description = "Number of ACTIVE sales.", example = "2") Long active,
        @Schema(description = "Number of COMPLETED sales.", example = "15") Long completed,
        @Schema(description = "Number of CANCELLED sales.", example = "3") Long cancelled)
        implements Serializable {
    }

    @Schema(description = "Top performing sale information.")
    public record TopSale(
        @Schema(description = "Flash sale identifier.", example = "5b3c3f18-2f88-4c38-8b35-9aa6d9b9f5af") UUID saleId,
        @Schema(description = "Flash sale title.", example = "Winter Deals") String title,
        @Schema(description = "Number of items sold.", example = "250") Long itemsSold,
        @Schema(description = "Total revenue from this sale.", example = "4999.50") BigDecimal revenue)
        implements Serializable {
    }
}
