package uk.co.aosd.flash.controllers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.co.aosd.flash.domain.OrderStatus;
import uk.co.aosd.flash.dto.ErrorResponseDto;
import uk.co.aosd.flash.dto.OrderDetailDto;
import uk.co.aosd.flash.dto.UpdateOrderStatusDto;
import uk.co.aosd.flash.exc.InvalidOrderStatusException;
import uk.co.aosd.flash.exc.OrderNotFoundException;
import uk.co.aosd.flash.services.OrderService;

/**
 * Admin REST API for order management.
 */
@RestController
@Profile("admin-service")
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(
    name = "Orders (Admin)",
    description = "Admin endpoints for managing orders."
)
public class AdminOrderRestApi {

    private static final Logger log = LoggerFactory.getLogger(AdminOrderRestApi.class);

    private final OrderService orderService;

    /**
     * List all orders with optional filters.
     *
     * @param status optional status filter
     * @param startDate optional start date filter (ISO-8601 format)
     * @param endDate optional end date filter (ISO-8601 format)
     * @param userId optional user ID filter
     * @return ResponseEntity with list of orders
     */
    @PreAuthorize("hasRole('ADMIN_USER')")
    @GetMapping("/orders")
    @Operation(
        summary = "List all orders",
        description = "Lists all orders with optional status, date range, and user filters."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of orders.",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = OrderDetailDto.class))
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid filters or date range.", content = @Content)
    })
    public ResponseEntity<List<OrderDetailDto>> getAllOrders(
        @Parameter(description = "Optional status filter.", schema = @Schema(implementation = OrderStatus.class), example = "PAID")
        @RequestParam(required = false) final String status,
        @Parameter(description = "Optional filter start date (ISO-8601).", example = "2026-01-01T00:00:00Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime startDate,
        @Parameter(description = "Optional filter end date (ISO-8601).", example = "2026-12-31T23:59:59Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime endDate,
        @Parameter(description = "Optional user ID filter.", example = "9b2b8c2c-2f53-4a57-a07e-0a2b2b1de3a9")
        @RequestParam(required = false) final String userId) {

        log.info("Getting all orders with filters: status={}, startDate={}, endDate={}, userId={}",
            status, startDate, endDate, userId);

        OrderStatus orderStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                orderStatus = OrderStatus.valueOf(status.toUpperCase());
            } catch (final IllegalArgumentException e) {
                log.warn("Invalid status value: {}", status);
                return ResponseEntity.badRequest().build();
            }
        }

        UUID userUuid = null;
        if (userId != null && !userId.isEmpty()) {
            try {
                userUuid = UUID.fromString(userId);
            } catch (final IllegalArgumentException e) {
                log.warn("Invalid user ID format: {}", userId);
                return ResponseEntity.badRequest().build();
            }
        }

        try {
            final List<OrderDetailDto> orders = orderService.getAllOrders(orderStatus, startDate, endDate, userUuid);
            log.info("Returned {} order(s)", orders.size());
            return ResponseEntity.ok(orders);
        } catch (final IllegalArgumentException e) {
            log.warn("Invalid filter parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get order details by ID (admin view).
     *
     * @param id the order ID
     * @return ResponseEntity with order DTO, or 404 if not found
     */
    @GetMapping("/orders/{id}")
    @Operation(
        summary = "Get order by id",
        description = "Returns order details by id (admin view, no user ownership validation)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Order found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderDetailDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid UUID format.", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Order not found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
    })
    public ResponseEntity<OrderDetailDto> getOrderById(
        @Parameter(description = "Order identifier (UUID).", example = "2b8efb9f-6f89-4b2d-8c73-4b2f9d4d2e1a")
        @PathVariable final String id) {

        log.info("Getting order by ID: {}", id);

        try {
            final UUID uuid = UUID.fromString(id);
            final OrderDetailDto order = orderService.getOrderByIdForAdmin(uuid);
            log.info("Fetched order with id: {}", id);
            return ResponseEntity.ok(order);
        } catch (final IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", id);
            return ResponseEntity.badRequest().build();
        } catch (final OrderNotFoundException e) {
            log.info("Order not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Update order status.
     *
     * @param id the order ID
     * @param updateDto the update DTO containing the new status
     * @return ResponseEntity with no content on success, or error response
     */
    @PreAuthorize("hasRole('ADMIN_USER')")
    @PutMapping("/orders/{id}/status")
    @Operation(
        summary = "Update order status",
        description = "Updates an order status with proper stock adjustments for all valid transitions."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Order status updated.",
            content = @Content
        ),
        @ApiResponse(responseCode = "400", description = "Invalid UUID format, invalid status, or invalid transition.", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Order not found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Validation error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Stock operation failed or other server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
    })
    public ResponseEntity<Void> updateOrderStatus(
        @Parameter(description = "Order identifier (UUID).", example = "2b8efb9f-6f89-4b2d-8c73-4b2f9d4d2e1a")
        @PathVariable final String id,
        @Valid @RequestBody final UpdateOrderStatusDto updateDto) {

        log.info("Updating order {} status to {}", id, updateDto.status());

        try {
            final UUID uuid = UUID.fromString(id);
            orderService.updateOrderStatus(uuid, updateDto.status());
            log.info("Updated order {} status to {}", id, updateDto.status());
            return ResponseEntity.ok().build();
        } catch (final IllegalArgumentException e) {
            log.error("Invalid UUID format or validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (final OrderNotFoundException e) {
            log.info("Order not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (final InvalidOrderStatusException e) {
            log.warn("Invalid order status transition: orderId={}, currentStatus={}, requiredStatus={}, operation={}",
                e.getOrderId(), e.getCurrentStatus(), e.getRequiredStatus(), e.getOperation());
            return ResponseEntity.badRequest().build();
        } catch (final IllegalStateException e) {
            log.error("Stock operation failed for order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
