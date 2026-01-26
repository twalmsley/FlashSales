package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for product performance metrics.
 */
@Schema(
    name = "ProductPerformance",
    description = "Product performance metrics and statistics."
)
public record ProductPerformanceDto(
    @Schema(description = "Total number of products in catalog.", example = "100")
    Long totalProducts,
    @Schema(description = "Top selling products by quantity sold.")
    List<TopProduct> topProductsByQuantity,
    @Schema(description = "Top products by revenue.")
    List<TopProduct> topProductsByRevenue,
    @Schema(description = "Average products per sale.", example = "4.5")
    BigDecimal averageProductsPerSale,
    @Schema(description = "Total physical stock across all products.", example = "10000")
    Long totalPhysicalStock,
    @Schema(description = "Total reserved stock across all products.", example = "2500")
    Long totalReservedStock,
    @Schema(description = "Stock utilization rate (reserved / total physical stock).", example = "0.25")
    BigDecimal stockUtilizationRate,
    @Schema(description = "Products with low stock (below threshold).")
    List<LowStockProduct> lowStockProducts)
    implements Serializable {

    @Schema(description = "Top performing product information.")
    public record TopProduct(
        @Schema(description = "Product identifier.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID productId,
        @Schema(description = "Product name.", example = "Wireless Mouse")
        String productName,
        @Schema(description = "Quantity sold.", example = "500")
        Long quantitySold,
        @Schema(description = "Total revenue from this product.", example = "9995.00")
        BigDecimal revenue)
        implements Serializable {
    }

    @Schema(description = "Product with low stock alert.")
    public record LowStockProduct(
        @Schema(description = "Product identifier.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID productId,
        @Schema(description = "Product name.", example = "Wireless Mouse")
        String productName,
        @Schema(description = "Current physical stock.", example = "5")
        Integer currentStock,
        @Schema(description = "Reserved stock.", example = "2")
        Integer reservedStock,
        @Schema(description = "Available stock (physical - reserved).", example = "3")
        Integer availableStock)
        implements Serializable {
    }
}
