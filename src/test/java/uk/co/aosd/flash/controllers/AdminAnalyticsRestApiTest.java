package uk.co.aosd.flash.controllers;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.co.aosd.flash.config.TestSecurityConfig;
import uk.co.aosd.flash.dto.OrderStatisticsDto;
import uk.co.aosd.flash.dto.ProductPerformanceDto;
import uk.co.aosd.flash.dto.RevenueMetricsDto;
import uk.co.aosd.flash.dto.SalesMetricsDto;
import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;
import uk.co.aosd.flash.services.AnalyticsService;
import uk.co.aosd.flash.services.JwtTokenProvider;

/**
 * Admin Analytics REST API test.
 */
@WebMvcTest(controllers = AdminAnalyticsRestApi.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ ErrorMapper.class, GlobalExceptionHandler.class, TestSecurityConfig.class })
@ActiveProfiles({"test", "admin-service"})
public class AdminAnalyticsRestApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AnalyticsService analyticsService;

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(analyticsService);
    }

    // GET /api/v1/admin/analytics/sales tests

    @Test
    public void shouldReturn200WithSalesMetricsNoDateRange() throws Exception {
        final SalesMetricsDto metrics = createTestSalesMetrics();

        when(analyticsService.getSalesMetrics(null, null)).thenReturn(metrics);

        mockMvc.perform(get("/api/v1/admin/analytics/sales"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalSales").value(25))
            .andExpect(jsonPath("$.salesByStatus.draft").value(5))
            .andExpect(jsonPath("$.salesByStatus.active").value(2))
            .andExpect(jsonPath("$.salesByStatus.completed").value(15))
            .andExpect(jsonPath("$.salesByStatus.cancelled").value(3))
            .andExpect(jsonPath("$.totalItemsSold").value(1500))
            .andExpect(jsonPath("$.totalItemsAllocated").value(2000));

        verify(analyticsService, times(1)).getSalesMetrics(null, null);
    }

    @Test
    public void shouldReturn200WithSalesMetricsWithDateRange() throws Exception {
        final OffsetDateTime startDate = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endDate = OffsetDateTime.of(2026, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        final SalesMetricsDto metrics = createTestSalesMetrics();

        when(analyticsService.getSalesMetrics(startDate, endDate)).thenReturn(metrics);

        mockMvc.perform(get("/api/v1/admin/analytics/sales")
            .param("startDate", "2026-01-01T00:00:00Z")
            .param("endDate", "2026-01-31T23:59:59Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalSales").value(25));

        verify(analyticsService, times(1)).getSalesMetrics(startDate, endDate);
    }

    @Test
    public void shouldReturn400ForInvalidDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/sales")
            .param("startDate", "2026-01-31T23:59:59Z")
            .param("endDate", "2026-01-01T00:00:00Z"))
            .andExpect(status().isBadRequest());

        verify(analyticsService, times(0)).getSalesMetrics(Mockito.any(), Mockito.any());
    }

    // GET /api/v1/admin/analytics/revenue tests

    @Test
    public void shouldReturn200WithRevenueMetricsNoDateRange() throws Exception {
        final RevenueMetricsDto metrics = createTestRevenueMetrics();

        when(analyticsService.getRevenueMetrics(null, null)).thenReturn(metrics);

        mockMvc.perform(get("/api/v1/admin/analytics/revenue"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRevenue").value(125000.00))
            .andExpect(jsonPath("$.totalPaidOrders").value(500))
            .andExpect(jsonPath("$.averageOrderValue").value(250.00));

        verify(analyticsService, times(1)).getRevenueMetrics(null, null);
    }

    @Test
    public void shouldReturn200WithRevenueMetricsWithDateRange() throws Exception {
        final OffsetDateTime startDate = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endDate = OffsetDateTime.of(2026, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        final RevenueMetricsDto metrics = createTestRevenueMetrics();

        when(analyticsService.getRevenueMetrics(startDate, endDate)).thenReturn(metrics);

        mockMvc.perform(get("/api/v1/admin/analytics/revenue")
            .param("startDate", "2026-01-01T00:00:00Z")
            .param("endDate", "2026-01-31T23:59:59Z"))
            .andExpect(status().isOk());

        verify(analyticsService, times(1)).getRevenueMetrics(startDate, endDate);
    }

    @Test
    public void shouldReturn400ForInvalidDateRangeInRevenue() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/revenue")
            .param("startDate", "2026-01-31T23:59:59Z")
            .param("endDate", "2026-01-01T00:00:00Z"))
            .andExpect(status().isBadRequest());

        verify(analyticsService, times(0)).getRevenueMetrics(Mockito.any(), Mockito.any());
    }

    // GET /api/v1/admin/analytics/products tests

    @Test
    public void shouldReturn200WithProductPerformance() throws Exception {
        final ProductPerformanceDto metrics = createTestProductPerformance();

        when(analyticsService.getProductPerformance()).thenReturn(metrics);

        mockMvc.perform(get("/api/v1/admin/analytics/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalProducts").value(100))
            .andExpect(jsonPath("$.totalPhysicalStock").value(10000))
            .andExpect(jsonPath("$.totalReservedStock").value(2500));

        verify(analyticsService, times(1)).getProductPerformance();
    }

    // GET /api/v1/admin/analytics/orders tests

    @Test
    public void shouldReturn200WithOrderStatisticsNoDateRange() throws Exception {
        final OrderStatisticsDto statistics = createTestOrderStatistics();

        when(analyticsService.getOrderStatistics(null, null)).thenReturn(statistics);

        mockMvc.perform(get("/api/v1/admin/analytics/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalOrders").value(1000))
            .andExpect(jsonPath("$.ordersByStatus.pending").value(100))
            .andExpect(jsonPath("$.ordersByStatus.paid").value(800))
            .andExpect(jsonPath("$.ordersByStatus.failed").value(50))
            .andExpect(jsonPath("$.ordersByStatus.refunded").value(30))
            .andExpect(jsonPath("$.ordersByStatus.dispatched").value(20));

        verify(analyticsService, times(1)).getOrderStatistics(null, null);
    }

    @Test
    public void shouldReturn200WithOrderStatisticsWithDateRange() throws Exception {
        final OffsetDateTime startDate = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endDate = OffsetDateTime.of(2026, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        final OrderStatisticsDto statistics = createTestOrderStatistics();

        when(analyticsService.getOrderStatistics(startDate, endDate)).thenReturn(statistics);

        mockMvc.perform(get("/api/v1/admin/analytics/orders")
            .param("startDate", "2026-01-01T00:00:00Z")
            .param("endDate", "2026-01-31T23:59:59Z"))
            .andExpect(status().isOk());

        verify(analyticsService, times(1)).getOrderStatistics(startDate, endDate);
    }

    @Test
    public void shouldReturn400ForInvalidDateRangeInOrders() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/orders")
            .param("startDate", "2026-01-31T23:59:59Z")
            .param("endDate", "2026-01-01T00:00:00Z"))
            .andExpect(status().isBadRequest());

        verify(analyticsService, times(0)).getOrderStatistics(Mockito.any(), Mockito.any());
    }

    // Helper methods

    private SalesMetricsDto createTestSalesMetrics() {
        final SalesMetricsDto.SalesByStatus salesByStatus = new SalesMetricsDto.SalesByStatus(5L, 2L, 15L, 3L);
        return new SalesMetricsDto(
            25L,
            salesByStatus,
            1500L,
            BigDecimal.valueOf(60.0),
            2000L,
            BigDecimal.valueOf(0.75),
            new ArrayList<>());
    }

    private RevenueMetricsDto createTestRevenueMetrics() {
        final BigDecimal totalRevenue = new BigDecimal("125000.00");
        final BigDecimal refundedRevenue = new BigDecimal("5000.00");
        final RevenueMetricsDto.RevenueByStatus revenueByStatus = new RevenueMetricsDto.RevenueByStatus(
            totalRevenue, refundedRevenue);
        return new RevenueMetricsDto(
            totalRevenue,
            revenueByStatus,
            500L,
            BigDecimal.valueOf(250.00),
            refundedRevenue,
            BigDecimal.valueOf(0.04));
    }

    private ProductPerformanceDto createTestProductPerformance() {
        return new ProductPerformanceDto(
            100L,
            new ArrayList<>(),
            new ArrayList<>(),
            BigDecimal.valueOf(4.5),
            10000L,
            2500L,
            BigDecimal.valueOf(0.25),
            new ArrayList<>());
    }

    private OrderStatisticsDto createTestOrderStatistics() {
        final OrderStatisticsDto.OrdersByStatus ordersByStatus = new OrderStatisticsDto.OrdersByStatus(
            100L, 800L, 50L, 30L, 20L, 10L);
        return new OrderStatisticsDto(
            1000L,
            ordersByStatus,
            2500L,
            BigDecimal.valueOf(2.5),
            800L,
            50L,
            BigDecimal.valueOf(0.941));
    }
}
