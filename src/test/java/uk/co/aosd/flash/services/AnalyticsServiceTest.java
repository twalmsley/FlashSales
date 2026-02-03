package uk.co.aosd.flash.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
 * Test the Analytics Service.
 */
public class AnalyticsServiceTest {

    private OrderRepository orderRepository;
    private FlashSaleRepository flashSaleRepository;
    private ProductRepository productRepository;
    private AnalyticsService analyticsService;

    @BeforeEach
    public void beforeEach() {
        orderRepository = Mockito.mock(OrderRepository.class);
        flashSaleRepository = Mockito.mock(FlashSaleRepository.class);
        productRepository = Mockito.mock(ProductRepository.class);

        analyticsService = new AnalyticsService(
            orderRepository,
            flashSaleRepository,
            productRepository);
    }

    @Test
    public void shouldGetSalesMetricsSuccessfully() {
        // Setup
        when(flashSaleRepository.countByStatus(SaleStatus.DRAFT)).thenReturn(5L);
        when(flashSaleRepository.countByStatus(SaleStatus.ACTIVE)).thenReturn(2L);
        when(flashSaleRepository.countByStatus(SaleStatus.COMPLETED)).thenReturn(15L);
        when(flashSaleRepository.countByStatus(SaleStatus.CANCELLED)).thenReturn(3L);
        when(flashSaleRepository.calculateTotalItemsSold(isNull(), isNull())).thenReturn(1500L);
        when(flashSaleRepository.calculateTotalItemsAllocated(isNull(), isNull())).thenReturn(2000L);
        when(flashSaleRepository.findTopSalesByItemsSold(eq(10), isNull(), isNull())).thenReturn(new ArrayList<>());

        // Execute
        final SalesMetricsDto metrics = analyticsService.getSalesMetrics(null, null);

        // Verify
        assertNotNull(metrics);
        assertEquals(25L, metrics.totalSales());
        assertEquals(5L, metrics.salesByStatus().draft());
        assertEquals(2L, metrics.salesByStatus().active());
        assertEquals(15L, metrics.salesByStatus().completed());
        assertEquals(3L, metrics.salesByStatus().cancelled());
        assertEquals(1500L, metrics.totalItemsSold());
        assertEquals(2000L, metrics.totalItemsAllocated());
        assertTrue(metrics.conversionRate().compareTo(BigDecimal.valueOf(0.75)) == 0);
    }

    @Test
    public void shouldGetSalesMetricsWithDateRange() {
        final OffsetDateTime startDate = OffsetDateTime.now().minusDays(30);
        final OffsetDateTime endDate = OffsetDateTime.now();

        // Setup
        when(flashSaleRepository.countByStatus(SaleStatus.DRAFT)).thenReturn(2L);
        when(flashSaleRepository.countByStatus(SaleStatus.ACTIVE)).thenReturn(1L);
        when(flashSaleRepository.countByStatus(SaleStatus.COMPLETED)).thenReturn(10L);
        when(flashSaleRepository.countByStatus(SaleStatus.CANCELLED)).thenReturn(1L);
        when(flashSaleRepository.calculateTotalItemsSold(eq(startDate), eq(endDate))).thenReturn(800L);
        when(flashSaleRepository.calculateTotalItemsAllocated(eq(startDate), eq(endDate))).thenReturn(1000L);
        when(flashSaleRepository.findTopSalesByItemsSold(eq(10), eq(startDate), eq(endDate)))
            .thenReturn(new ArrayList<>());

        // Execute
        final SalesMetricsDto metrics = analyticsService.getSalesMetrics(startDate, endDate);

        // Verify
        assertNotNull(metrics);
        assertEquals(14L, metrics.totalSales());
        assertEquals(800L, metrics.totalItemsSold());
    }

    @Test
    public void shouldGetRevenueMetricsSuccessfully() {
        // Setup
        final BigDecimal totalRevenue = new BigDecimal("125000.00");
        final BigDecimal refundedRevenue = new BigDecimal("5000.00");
        when(orderRepository.calculateTotalRevenue(eq(OrderStatus.PAID), isNull(), isNull()))
            .thenReturn(totalRevenue);
        when(orderRepository.calculateTotalRevenue(eq(OrderStatus.REFUNDED), isNull(), isNull()))
            .thenReturn(refundedRevenue);
        when(orderRepository.countOrdersByStatus(eq(OrderStatus.PAID), isNull(), isNull()))
            .thenReturn(500L);
        when(orderRepository.calculateAverageOrderValue(isNull(), isNull()))
            .thenReturn(new BigDecimal("250.00"));

        // Execute
        final RevenueMetricsDto metrics = analyticsService.getRevenueMetrics(null, null);

        // Verify
        assertNotNull(metrics);
        assertEquals(totalRevenue, metrics.totalRevenue());
        assertEquals(refundedRevenue, metrics.totalRefundedRevenue());
        assertEquals(500L, metrics.totalPaidOrders());
        assertEquals(new BigDecimal("250.00"), metrics.averageOrderValue());
    }

    @Test
    public void shouldGetProductPerformanceSuccessfully() {
        // Setup
        when(productRepository.count()).thenReturn(100L);
        when(productRepository.calculateTotalPhysicalStock()).thenReturn(10000L);
        when(productRepository.calculateTotalReservedStock()).thenReturn(2500L);
        when(flashSaleRepository.calculateAverageProductsPerSale()).thenReturn(4.5);
        when(orderRepository.findTopProductsByQuantity(eq(10), isNull(), isNull()))
            .thenReturn(new ArrayList<>());
        when(orderRepository.findTopProductsByRevenue(eq(10), isNull(), isNull()))
            .thenReturn(new ArrayList<>());
        when(productRepository.findProductsWithLowStock(eq(10))).thenReturn(new ArrayList<>());

        // Execute
        final ProductPerformanceDto metrics = analyticsService.getProductPerformance();

        // Verify
        assertNotNull(metrics);
        assertEquals(100L, metrics.totalProducts());
        assertEquals(10000L, metrics.totalPhysicalStock());
        assertEquals(2500L, metrics.totalReservedStock());
        assertTrue(metrics.stockUtilizationRate().compareTo(BigDecimal.valueOf(0.25)) == 0);
    }

    @Test
    public void shouldGetProductPerformanceWithTopProducts() {
        final UUID productId1 = UUID.randomUUID();
        final UUID productId2 = UUID.randomUUID();

        // Setup
        when(productRepository.count()).thenReturn(100L);
        when(productRepository.calculateTotalPhysicalStock()).thenReturn(10000L);
        when(productRepository.calculateTotalReservedStock()).thenReturn(2500L);
        when(flashSaleRepository.calculateAverageProductsPerSale()).thenReturn(4.5);

        // Top products by quantity
        final List<Object[]> topByQuantity = new ArrayList<>();
        topByQuantity.add(new Object[] { productId1, "Product 1", 500L });
        topByQuantity.add(new Object[] { productId2, "Product 2", 300L });
        when(orderRepository.findTopProductsByQuantity(eq(10), isNull(), isNull()))
            .thenReturn(topByQuantity);
        when(orderRepository.calculateRevenueForProduct(eq(productId1), isNull(), isNull()))
            .thenReturn(new BigDecimal("9995.00"));
        when(orderRepository.calculateRevenueForProduct(eq(productId2), isNull(), isNull()))
            .thenReturn(new BigDecimal("5997.00"));

        // Top products by revenue
        final List<Object[]> topByRevenue = new ArrayList<>();
        topByRevenue.add(new Object[] { productId1, "Product 1", new BigDecimal("9995.00") });
        topByRevenue.add(new Object[] { productId2, "Product 2", new BigDecimal("5997.00") });
        when(orderRepository.findTopProductsByRevenue(eq(10), isNull(), isNull()))
            .thenReturn(topByRevenue);
        when(orderRepository.calculateQuantityForProduct(eq(productId1), isNull(), isNull()))
            .thenReturn(500L);
        when(orderRepository.calculateQuantityForProduct(eq(productId2), isNull(), isNull()))
            .thenReturn(300L);

        when(productRepository.findProductsWithLowStock(eq(10))).thenReturn(new ArrayList<>());

        // Execute
        final ProductPerformanceDto metrics = analyticsService.getProductPerformance();

        // Verify
        assertNotNull(metrics);
        assertEquals(2, metrics.topProductsByQuantity().size());
        assertEquals("Product 1", metrics.topProductsByQuantity().get(0).productName());
        assertEquals(500L, metrics.topProductsByQuantity().get(0).quantitySold());
        assertEquals(2, metrics.topProductsByRevenue().size());
    }

    @Test
    public void shouldGetOrderStatisticsSuccessfully() {
        // Setup
        when(orderRepository.countOrdersByStatus(eq(OrderStatus.PENDING), isNull(), isNull()))
            .thenReturn(100L);
        when(orderRepository.countOrdersByStatus(eq(OrderStatus.PAID), isNull(), isNull()))
            .thenReturn(800L);
        when(orderRepository.countOrdersByStatus(eq(OrderStatus.FAILED), isNull(), isNull()))
            .thenReturn(50L);
        when(orderRepository.countOrdersByStatus(eq(OrderStatus.REFUNDED), isNull(), isNull()))
            .thenReturn(30L);
        when(orderRepository.countOrdersByStatus(eq(OrderStatus.DISPATCHED), isNull(), isNull()))
            .thenReturn(20L);
        when(orderRepository.countOrdersByStatus(eq(OrderStatus.CANCELLED), isNull(), isNull()))
            .thenReturn(10L);
        when(orderRepository.calculateTotalOrderQuantity(isNull(), isNull())).thenReturn(2500L);

        // Execute
        final OrderStatisticsDto statistics = analyticsService.getOrderStatistics(null, null);

        // Verify
        assertNotNull(statistics);
        assertEquals(1010L, statistics.totalOrders());
        assertEquals(100L, statistics.ordersByStatus().pending());
        assertEquals(800L, statistics.ordersByStatus().paid());
        assertEquals(50L, statistics.ordersByStatus().failed());
        assertEquals(30L, statistics.ordersByStatus().refunded());
        assertEquals(20L, statistics.ordersByStatus().dispatched());
        assertEquals(10L, statistics.ordersByStatus().cancelled());
        assertEquals(2500L, statistics.totalOrderQuantity());
        assertEquals(800L, statistics.paidOrders());
        assertEquals(50L, statistics.failedOrders());
    }

    @Test
    public void shouldHandleEmptyData() {
        // Setup - all empty
        when(flashSaleRepository.countByStatus(any())).thenReturn(0L);
        when(flashSaleRepository.calculateTotalItemsSold(any(), any())).thenReturn(0L);
        when(flashSaleRepository.calculateTotalItemsAllocated(any(), any())).thenReturn(0L);
        when(flashSaleRepository.findTopSalesByItemsSold(anyInt(), any(), any()))
            .thenReturn(new ArrayList<>());
        when(orderRepository.calculateTotalRevenue(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(orderRepository.countOrdersByStatus(any(), any(), any())).thenReturn(0L);
        when(orderRepository.calculateAverageOrderValue(any(), any())).thenReturn(BigDecimal.ZERO);
        when(productRepository.count()).thenReturn(0L);
        when(productRepository.calculateTotalPhysicalStock()).thenReturn(0L);
        when(productRepository.calculateTotalReservedStock()).thenReturn(0L);
        when(flashSaleRepository.calculateAverageProductsPerSale()).thenReturn(0.0);
        when(orderRepository.findTopProductsByQuantity(anyInt(), any(), any()))
            .thenReturn(new ArrayList<>());
        when(orderRepository.findTopProductsByRevenue(anyInt(), any(), any()))
            .thenReturn(new ArrayList<>());
        when(orderRepository.calculateTotalOrderQuantity(any(), any())).thenReturn(0L);
        when(productRepository.findProductsWithLowStock(anyInt())).thenReturn(new ArrayList<>());

        // Execute
        final SalesMetricsDto salesMetrics = analyticsService.getSalesMetrics(null, null);
        final RevenueMetricsDto revenueMetrics = analyticsService.getRevenueMetrics(null, null);
        final ProductPerformanceDto productMetrics = analyticsService.getProductPerformance();
        final OrderStatisticsDto orderStats = analyticsService.getOrderStatistics(null, null);

        // Verify - should not throw and should return zero values
        assertNotNull(salesMetrics);
        assertEquals(0L, salesMetrics.totalSales());
        assertNotNull(revenueMetrics);
        assertEquals(BigDecimal.ZERO, revenueMetrics.totalRevenue());
        assertNotNull(productMetrics);
        assertEquals(0L, productMetrics.totalProducts());
        assertNotNull(orderStats);
        assertEquals(0L, orderStats.totalOrders());
    }

    @Test
    public void shouldGetProductPerformanceWithLowStockProducts() {
        final UUID productId1 = UUID.randomUUID();
        final UUID productId2 = UUID.randomUUID();
        final Product lowStockProduct1 = new Product(productId1, "Low Stock Product 1", "Desc", 5, BigDecimal.valueOf(10.00), 2);
        final Product lowStockProduct2 = new Product(productId2, "Low Stock Product 2", "Desc", 8, BigDecimal.valueOf(15.00), 3);

        // Setup
        when(productRepository.count()).thenReturn(100L);
        when(productRepository.calculateTotalPhysicalStock()).thenReturn(10000L);
        when(productRepository.calculateTotalReservedStock()).thenReturn(2500L);
        when(flashSaleRepository.calculateAverageProductsPerSale()).thenReturn(4.5);
        when(orderRepository.findTopProductsByQuantity(anyInt(), any(), any()))
            .thenReturn(new ArrayList<>());
        when(orderRepository.findTopProductsByRevenue(anyInt(), any(), any()))
            .thenReturn(new ArrayList<>());
        when(productRepository.findProductsWithLowStock(eq(10)))
            .thenReturn(List.of(lowStockProduct1, lowStockProduct2));

        // Execute
        final ProductPerformanceDto metrics = analyticsService.getProductPerformance();

        // Verify
        assertNotNull(metrics);
        assertEquals(2, metrics.lowStockProducts().size());
        assertEquals("Low Stock Product 1", metrics.lowStockProducts().get(0).productName());
        assertEquals(5, metrics.lowStockProducts().get(0).currentStock());
        assertEquals(2, metrics.lowStockProducts().get(0).reservedStock());
        assertEquals(3, metrics.lowStockProducts().get(0).availableStock());
    }
}
