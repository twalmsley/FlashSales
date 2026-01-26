package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for revenue reporting and financial metrics.
 */
@Schema(name = "RevenueMetrics", description = "Revenue reporting and financial metrics.")
public record RevenueMetricsDto(
    @Schema(description = "Total revenue from all PAID orders.", example = "125000.00") BigDecimal totalRevenue,
    @Schema(description = "Revenue by order status.") RevenueByStatus revenueByStatus,
    @Schema(description = "Total number of PAID orders.", example = "500") Long totalPaidOrders,
    @Schema(description = "Average order value (total revenue / paid orders).", example = "250.00") BigDecimal averageOrderValue,
    @Schema(description = "Total refunded revenue.", example = "5000.00") BigDecimal totalRefundedRevenue,
    @Schema(description = "Refund rate (refunded revenue / total revenue).", example = "0.04") BigDecimal refundRate)
    implements Serializable {

    @Schema(description = "Revenue breakdown by order status.")
    public record RevenueByStatus(
        @Schema(description = "Revenue from PAID orders.", example = "125000.00") BigDecimal paid,
        @Schema(description = "Revenue from REFUNDED orders.", example = "5000.00") BigDecimal refunded)
        implements Serializable {
    }
}
