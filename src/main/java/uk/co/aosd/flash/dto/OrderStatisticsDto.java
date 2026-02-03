package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for order statistics and patterns.
 */
@Schema(name = "OrderStatistics", description = "Order statistics and patterns.")
public record OrderStatisticsDto(
    @Schema(description = "Total number of orders.", example = "1000") Long totalOrders,
    @Schema(description = "Orders count by status.") OrdersByStatus ordersByStatus,
    @Schema(description = "Total quantity of items ordered.", example = "2500") Long totalOrderQuantity,
    @Schema(description = "Average order quantity (total quantity / total orders).", example = "2.5") BigDecimal averageOrderQuantity,
    @Schema(description = "Number of PAID orders.", example = "800") Long paidOrders,
    @Schema(description = "Number of FAILED orders.", example = "50") Long failedOrders,
    @Schema(description = "Order success rate (PAID / (PAID + FAILED)).", example = "0.941") BigDecimal successRate)
    implements Serializable {

    @Schema(description = "Order count by status.")
    public record OrdersByStatus(
        @Schema(description = "Number of PENDING orders.", example = "100") Long pending,
        @Schema(description = "Number of PAID orders.", example = "800") Long paid,
        @Schema(description = "Number of FAILED orders.", example = "50") Long failed,
        @Schema(description = "Number of REFUNDED orders.", example = "30") Long refunded,
        @Schema(description = "Number of DISPATCHED orders.", example = "20") Long dispatched,
        @Schema(description = "Number of CANCELLED orders.", example = "10") Long cancelled)
        implements Serializable {
    }
}
