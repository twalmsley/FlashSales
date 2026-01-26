package uk.co.aosd.flash.controllers;

import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.co.aosd.flash.dto.OrderStatisticsDto;
import uk.co.aosd.flash.dto.ProductPerformanceDto;
import uk.co.aosd.flash.dto.RevenueMetricsDto;
import uk.co.aosd.flash.dto.SalesMetricsDto;
import uk.co.aosd.flash.services.AnalyticsService;

/**
 * Admin REST API for analytics and reporting.
 */
@RestController
@Profile("admin-service")
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@Tag(
    name = "Analytics (Admin)",
    description = "Admin endpoints for analytics and reporting."
)
public class AdminAnalyticsRestApi {

    private static final Logger log = LoggerFactory.getLogger(AdminAnalyticsRestApi.class);

    private final AnalyticsService analyticsService;

    /**
     * Get sales performance metrics.
     *
     * @param startDate optional start date filter (ISO-8601 format)
     * @param endDate optional end date filter (ISO-8601 format)
     * @return ResponseEntity with sales metrics
     */
    @PreAuthorize("hasRole('ADMIN_USER')")
    @GetMapping("/sales")
    @Operation(
        summary = "Get sales performance metrics",
        description = "Returns sales performance metrics including total sales, items sold, conversion rates, and top performing sales."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Sales metrics retrieved successfully.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SalesMetricsDto.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid date range.", content = @Content)
    })
    public ResponseEntity<SalesMetricsDto> getSalesMetrics(
        @Parameter(description = "Optional filter start date (ISO-8601).", example = "2026-01-01T00:00:00Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime startDate,
        @Parameter(description = "Optional filter end date (ISO-8601).", example = "2026-12-31T23:59:59Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime endDate) {

        log.info("Getting sales metrics with date range: {} to {}", startDate, endDate);

        // Validate date range if both are provided
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            log.warn("Invalid date range: startDate {} is after endDate {}", startDate, endDate);
            return ResponseEntity.badRequest().build();
        }

        try {
            final SalesMetricsDto metrics = analyticsService.getSalesMetrics(startDate, endDate);
            log.info("Retrieved sales metrics: {} total sales", metrics.totalSales());
            return ResponseEntity.ok(metrics);
        } catch (final IllegalArgumentException e) {
            log.warn("Invalid parameters for sales metrics: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get revenue metrics.
     *
     * @param startDate optional start date filter (ISO-8601 format)
     * @param endDate optional end date filter (ISO-8601 format)
     * @return ResponseEntity with revenue metrics
     */
    @PreAuthorize("hasRole('ADMIN_USER')")
    @GetMapping("/revenue")
    @Operation(
        summary = "Get revenue reporting metrics",
        description = "Returns revenue reporting and financial metrics including total revenue, average order value, and refund rates."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Revenue metrics retrieved successfully.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RevenueMetricsDto.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid date range.", content = @Content)
    })
    public ResponseEntity<RevenueMetricsDto> getRevenueMetrics(
        @Parameter(description = "Optional filter start date (ISO-8601).", example = "2026-01-01T00:00:00Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime startDate,
        @Parameter(description = "Optional filter end date (ISO-8601).", example = "2026-12-31T23:59:59Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime endDate) {

        log.info("Getting revenue metrics with date range: {} to {}", startDate, endDate);

        // Validate date range if both are provided
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            log.warn("Invalid date range: startDate {} is after endDate {}", startDate, endDate);
            return ResponseEntity.badRequest().build();
        }

        try {
            final RevenueMetricsDto metrics = analyticsService.getRevenueMetrics(startDate, endDate);
            log.info("Retrieved revenue metrics: total revenue {}", metrics.totalRevenue());
            return ResponseEntity.ok(metrics);
        } catch (final IllegalArgumentException e) {
            log.warn("Invalid parameters for revenue metrics: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get product performance metrics.
     *
     * @return ResponseEntity with product performance metrics
     */
    @PreAuthorize("hasRole('ADMIN_USER')")
    @GetMapping("/products")
    @Operation(
        summary = "Get product performance metrics",
        description = "Returns product performance metrics including top selling products, stock utilization, and low stock alerts."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product performance metrics retrieved successfully.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProductPerformanceDto.class)
            )
        )
    })
    public ResponseEntity<ProductPerformanceDto> getProductPerformance() {
        log.info("Getting product performance metrics");

        try {
            final ProductPerformanceDto metrics = analyticsService.getProductPerformance();
            log.info("Retrieved product performance metrics: {} total products", metrics.totalProducts());
            return ResponseEntity.ok(metrics);
        } catch (final Exception e) {
            log.error("Error retrieving product performance metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get order statistics.
     *
     * @param startDate optional start date filter (ISO-8601 format)
     * @param endDate optional end date filter (ISO-8601 format)
     * @return ResponseEntity with order statistics
     */
    @PreAuthorize("hasRole('ADMIN_USER')")
    @GetMapping("/orders")
    @Operation(
        summary = "Get order statistics",
        description = "Returns order statistics including order counts by status, average order quantity, and success rates."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Order statistics retrieved successfully.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrderStatisticsDto.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid date range.", content = @Content)
    })
    public ResponseEntity<OrderStatisticsDto> getOrderStatistics(
        @Parameter(description = "Optional filter start date (ISO-8601).", example = "2026-01-01T00:00:00Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime startDate,
        @Parameter(description = "Optional filter end date (ISO-8601).", example = "2026-12-31T23:59:59Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime endDate) {

        log.info("Getting order statistics with date range: {} to {}", startDate, endDate);

        // Validate date range if both are provided
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            log.warn("Invalid date range: startDate {} is after endDate {}", startDate, endDate);
            return ResponseEntity.badRequest().build();
        }

        try {
            final OrderStatisticsDto statistics = analyticsService.getOrderStatistics(startDate, endDate);
            log.info("Retrieved order statistics: {} total orders", statistics.totalOrders());
            return ResponseEntity.ok(statistics);
        } catch (final IllegalArgumentException e) {
            log.warn("Invalid parameters for order statistics: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
