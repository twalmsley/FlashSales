package uk.co.aosd.flash.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.aosd.flash.domain.OrderStatus;
import uk.co.aosd.flash.domain.Product;
import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.OrderStatisticsDto;
import uk.co.aosd.flash.dto.ProductPerformanceDto;
import uk.co.aosd.flash.dto.RevenueMetricsDto;
import uk.co.aosd.flash.dto.SalesMetricsDto;
import uk.co.aosd.flash.repository.FlashSaleRepository;
import uk.co.aosd.flash.repository.OrderRepository;
import uk.co.aosd.flash.repository.ProductRepository;

/**
 * Service for analytics and reporting.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private static final int DEFAULT_TOP_LIMIT = 10;
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    private final OrderRepository orderRepository;
    private final FlashSaleRepository flashSaleRepository;
    private final ProductRepository productRepository;

    /**
     * Get sales performance metrics.
     *
     * @param startDate
     *            optional start date filter
     * @param endDate
     *            optional end date filter
     * @return sales metrics DTO
     */
    @Cacheable(value = "analytics:sales", key = "(#startDate != null ? #startDate.toString() : 'null') + ':' + (#endDate != null ? #endDate.toString() : 'null')")
    @Transactional(readOnly = true)
    public SalesMetricsDto getSalesMetrics(final OffsetDateTime startDate, final OffsetDateTime endDate) {
        log.info("Calculating sales metrics for date range: {} to {}", startDate, endDate);

        // Count sales by status
        final Long draftCount = flashSaleRepository.countByStatus(SaleStatus.DRAFT);
        final Long activeCount = flashSaleRepository.countByStatus(SaleStatus.ACTIVE);
        final Long completedCount = flashSaleRepository.countByStatus(SaleStatus.COMPLETED);
        final Long cancelledCount = flashSaleRepository.countByStatus(SaleStatus.CANCELLED);
        final Long totalSales = draftCount + activeCount + completedCount + cancelledCount;

        // Calculate items sold and allocated
        final Long totalItemsSold = flashSaleRepository.calculateTotalItemsSold(startDate, endDate);
        final Long totalItemsAllocated = flashSaleRepository.calculateTotalItemsAllocated(startDate, endDate);

        // Calculate averages and rates
        final BigDecimal averageItemsSoldPerSale = totalSales > 0
            ? BigDecimal.valueOf(totalItemsSold).divide(BigDecimal.valueOf(totalSales), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        final BigDecimal conversionRate = totalItemsAllocated > 0
            ? BigDecimal.valueOf(totalItemsSold).divide(BigDecimal.valueOf(totalItemsAllocated), 4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Get top sales by items sold
        final List<Object[]> topSalesData = flashSaleRepository.findTopSalesByItemsSold(
            DEFAULT_TOP_LIMIT, startDate, endDate);
        final List<SalesMetricsDto.TopSale> topSales = topSalesData.stream()
            .map(row -> {
                final UUID saleId = (UUID) row[0];
                final String title = (String) row[1];
                final Long itemsSold = ((Number) row[2]).longValue();
                final BigDecimal revenue = (BigDecimal) row[3];
                return new SalesMetricsDto.TopSale(saleId, title, itemsSold, revenue);
            })
            .toList();

        final SalesMetricsDto.SalesByStatus salesByStatus = new SalesMetricsDto.SalesByStatus(
            draftCount, activeCount, completedCount, cancelledCount);

        return new SalesMetricsDto(
            totalSales,
            salesByStatus,
            totalItemsSold,
            averageItemsSoldPerSale,
            totalItemsAllocated,
            conversionRate,
            topSales);
    }

    /**
     * Get revenue metrics.
     *
     * @param startDate
     *            optional start date filter
     * @param endDate
     *            optional end date filter
     * @return revenue metrics DTO
     */
    @Cacheable(value = "analytics:revenue", key = "(#startDate != null ? #startDate.toString() : 'null') + ':' + (#endDate != null ? #endDate.toString() : 'null')")
    @Transactional(readOnly = true)
    public RevenueMetricsDto getRevenueMetrics(final OffsetDateTime startDate, final OffsetDateTime endDate) {
        log.info("Calculating revenue metrics for date range: {} to {}", startDate, endDate);

        // Calculate total revenue from PAID orders
        final BigDecimal totalRevenue = orderRepository.calculateTotalRevenue(
            OrderStatus.PAID, startDate, endDate);

        // Calculate refunded revenue
        final BigDecimal totalRefundedRevenue = orderRepository.calculateTotalRevenue(
            OrderStatus.REFUNDED, startDate, endDate);

        // Count paid orders
        final Long totalPaidOrders = orderRepository.countOrdersByStatus(
            OrderStatus.PAID, startDate, endDate);

        // Calculate average order value
        final BigDecimal averageOrderValue = orderRepository.calculateAverageOrderValue(startDate, endDate);

        // Calculate refund rate
        final BigDecimal refundRate = totalRevenue.compareTo(BigDecimal.ZERO) > 0
            ? totalRefundedRevenue.divide(totalRevenue, 4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        final RevenueMetricsDto.RevenueByStatus revenueByStatus = new RevenueMetricsDto.RevenueByStatus(
            totalRevenue, totalRefundedRevenue);

        return new RevenueMetricsDto(
            totalRevenue,
            revenueByStatus,
            totalPaidOrders,
            averageOrderValue,
            totalRefundedRevenue,
            refundRate);
    }

    /**
     * Get product performance metrics.
     *
     * @return product performance DTO
     */
    @Cacheable(value = "analytics:products", key = "'all'")
    @Transactional(readOnly = true)
    public ProductPerformanceDto getProductPerformance() {
        log.info("Calculating product performance metrics");

        // Count total products
        final Long totalProducts = productRepository.count();

        // Calculate total stock metrics
        final Long totalPhysicalStock = productRepository.calculateTotalPhysicalStock();
        final Long totalReservedStock = productRepository.calculateTotalReservedStock();

        // Calculate stock utilization rate
        final BigDecimal stockUtilizationRate = totalPhysicalStock > 0
            ? BigDecimal.valueOf(totalReservedStock).divide(BigDecimal.valueOf(totalPhysicalStock), 4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Get top products by quantity (all-time, no date filter)
        final List<Object[]> topProductsByQuantityData = orderRepository.findTopProductsByQuantity(
            DEFAULT_TOP_LIMIT, null, null);
        final List<ProductPerformanceDto.TopProduct> topProductsByQuantity = topProductsByQuantityData.stream()
            .map(row -> {
                final UUID productId = (UUID) row[0];
                final String productName = (String) row[1];
                final Long quantitySold = ((Number) row[2]).longValue();
                // Calculate revenue for this specific product
                final BigDecimal revenue = orderRepository.calculateRevenueForProduct(productId, null, null);
                return new ProductPerformanceDto.TopProduct(productId, productName, quantitySold, revenue);
            })
            .toList();

        // Get top products by revenue (all-time, no date filter)
        final List<Object[]> topProductsByRevenueData = orderRepository.findTopProductsByRevenue(
            DEFAULT_TOP_LIMIT, null, null);
        final List<ProductPerformanceDto.TopProduct> topProductsByRevenue = topProductsByRevenueData.stream()
            .map(row -> {
                final UUID productId = (UUID) row[0];
                final String productName = (String) row[1];
                final BigDecimal revenue = (BigDecimal) row[2];
                // Get quantity sold for this specific product
                final Long quantitySold = orderRepository.calculateQuantityForProduct(productId, null, null);
                return new ProductPerformanceDto.TopProduct(productId, productName, quantitySold, revenue);
            })
            .toList();

        // Calculate average products per sale
        final Double avgProductsPerSale = flashSaleRepository.calculateAverageProductsPerSale();
        final BigDecimal averageProductsPerSale = avgProductsPerSale != null
            ? BigDecimal.valueOf(avgProductsPerSale).setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Find products with low stock
        final List<Product> lowStockProducts = productRepository.findProductsWithLowStock(DEFAULT_LOW_STOCK_THRESHOLD);
        final List<ProductPerformanceDto.LowStockProduct> lowStockProductList = lowStockProducts.stream()
            .map(p -> {
                final int availableStock = p.getTotalPhysicalStock() - p.getReservedCount();
                return new ProductPerformanceDto.LowStockProduct(
                    p.getId(), p.getName(), p.getTotalPhysicalStock(), p.getReservedCount(), availableStock);
            })
            .toList();

        return new ProductPerformanceDto(
            totalProducts,
            topProductsByQuantity,
            topProductsByRevenue,
            averageProductsPerSale,
            totalPhysicalStock,
            totalReservedStock,
            stockUtilizationRate,
            lowStockProductList);
    }

    /**
     * Get order statistics.
     *
     * @param startDate
     *            optional start date filter
     * @param endDate
     *            optional end date filter
     * @return order statistics DTO
     */
    @Cacheable(value = "analytics:orders", key = "(#startDate != null ? #startDate.toString() : 'null') + ':' + (#endDate != null ? #endDate.toString() : 'null')")
    @Transactional(readOnly = true)
    public OrderStatisticsDto getOrderStatistics(final OffsetDateTime startDate, final OffsetDateTime endDate) {
        log.info("Calculating order statistics for date range: {} to {}", startDate, endDate);

        // Count orders by status
        final Long pendingCount = orderRepository.countOrdersByStatus(OrderStatus.PENDING, startDate, endDate);
        final Long paidCount = orderRepository.countOrdersByStatus(OrderStatus.PAID, startDate, endDate);
        final Long failedCount = orderRepository.countOrdersByStatus(OrderStatus.FAILED, startDate, endDate);
        final Long refundedCount = orderRepository.countOrdersByStatus(OrderStatus.REFUNDED, startDate, endDate);
        final Long dispatchedCount = orderRepository.countOrdersByStatus(OrderStatus.DISPATCHED, startDate, endDate);
        final Long cancelledCount = orderRepository.countOrdersByStatus(OrderStatus.CANCELLED, startDate, endDate);

        final Long totalOrders = pendingCount + paidCount + failedCount + refundedCount + dispatchedCount + cancelledCount;

        // Calculate total order quantity
        final Long totalOrderQuantity = orderRepository.calculateTotalOrderQuantity(startDate, endDate);

        // Calculate average order quantity
        final BigDecimal averageOrderQuantity = totalOrders > 0
            ? BigDecimal.valueOf(totalOrderQuantity).divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Calculate success rate (PAID / (PAID + FAILED))
        final long successfulOrders = paidCount;
        final long attemptedOrders = paidCount + failedCount;
        final BigDecimal successRate = attemptedOrders > 0
            ? BigDecimal.valueOf(successfulOrders).divide(BigDecimal.valueOf(attemptedOrders), 4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        final OrderStatisticsDto.OrdersByStatus ordersByStatus = new OrderStatisticsDto.OrdersByStatus(
            pendingCount, paidCount, failedCount, refundedCount, dispatchedCount, cancelledCount);

        return new OrderStatisticsDto(
            totalOrders,
            ordersByStatus,
            totalOrderQuantity,
            averageOrderQuantity,
            paidCount,
            failedCount,
            successRate);
    }
}
