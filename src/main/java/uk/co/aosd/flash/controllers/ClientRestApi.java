package uk.co.aosd.flash.controllers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import uk.co.aosd.flash.dto.ClientActiveSaleDto;
import uk.co.aosd.flash.dto.ClientDraftSaleDto;
import uk.co.aosd.flash.dto.ClientProductDto;
import uk.co.aosd.flash.dto.CreateOrderDto;
import uk.co.aosd.flash.dto.ErrorResponseDto;
import uk.co.aosd.flash.dto.OrderDetailDto;
import uk.co.aosd.flash.dto.OrderResponseDto;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.security.SecurityUtils;
import uk.co.aosd.flash.services.ActiveSalesService;
import uk.co.aosd.flash.services.DraftSalesService;
import uk.co.aosd.flash.services.OrderService;
import uk.co.aosd.flash.services.ProductsService;

/**
 * Client API.
 */
@RestController
@Profile("api-service")
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(
    name = "Client API",
    description = "Client-facing endpoints for browsing sales and placing/viewing orders."
)
public class ClientRestApi {

    private static Logger log = LoggerFactory.getLogger(ClientRestApi.class.getName());

    private final ProductsService service;

    private final ActiveSalesService activeSalesService;

    private final DraftSalesService draftSalesService;

    private final OrderService orderService;

    /**
     * Get a client's view of a product.
     *
     * @param id
     *            Product ID String
     * @return maybe a ClientProductDto
     */
    @GetMapping("/products/{id}")
    @Operation(
        summary = "Get product (client view)",
        description = "Returns a simplified product view for clients."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClientProductDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found.",
            content = @Content
        )
    })
    public ResponseEntity<Optional<ClientProductDto>> getProductById(
        @Parameter(description = "Product identifier.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @PathVariable final String id) {
        // Logic to return a single product by ID
        final Optional<ProductDto> productById = service.getProductById(id);
        if (productById.isEmpty()) {
            log.info("Failed to fetch product with id: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Optional.empty());
        }
        log.info("Fetched product with id: " + id);
        final ProductDto dto = productById.get();
        final ClientProductDto clientProductById = new ClientProductDto(id, dto.name(), dto.description(), dto.basePrice());
        return ResponseEntity.ok(Optional.of(clientProductById));
    }

    /**
     * Get all active sales with remaining stock.
     *
     * @return List of active sales
     */
    @GetMapping("/sales/active")
    @Operation(
        summary = "List active sales",
        description = "Returns all active flash sale items with remaining stock."
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of active sales.",
        content = @Content(
            mediaType = "application/json",
            array = @ArraySchema(schema = @Schema(implementation = ClientActiveSaleDto.class))
        )
    )
    public ResponseEntity<List<ClientActiveSaleDto>> getActiveSales() {
        log.info("Fetching active sales");
        final List<ClientActiveSaleDto> activeSales = activeSalesService.getActiveSales();
        log.info("Fetched {} active sales", activeSales.size());
        return ResponseEntity.ok(activeSales);
    }

    /**
     * Get all DRAFT flash sales coming up within the next N days.
     *
     * @param days
     *            the number of days to look ahead
     * @return List of draft sales
     */
    @GetMapping("/sales/draft/{days}")
    @Operation(
        summary = "List upcoming draft sales",
        description = "Returns draft (upcoming) flash sales that start within the next N days."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of upcoming draft sales.",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ClientDraftSaleDto.class))
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid 'days' parameter.",
            content = @Content
        )
    })
    public ResponseEntity<List<ClientDraftSaleDto>> getDraftSales(
        @Parameter(description = "Number of days to look ahead. Must be >= 0.", example = "7")
        @PathVariable final int days) {
        log.info("Fetching draft sales within the next {} days", days);
        if (days < 0) {
            log.warn("Invalid days parameter: {}", days);
            return ResponseEntity.badRequest().build();
        }
        final List<ClientDraftSaleDto> draftSales = draftSalesService.getDraftSalesWithinDays(days);
        log.info("Fetched {} draft sales within the next {} days", draftSales.size(), days);
        return ResponseEntity.ok(draftSales);
    }

    /**
     * Create a new order for an active sale.
     *
     * @param createOrderDto
     *            the order creation DTO
     * @return OrderResponseDto with order status
     */
    @PostMapping("/orders")
    @Operation(
        summary = "Create order",
        description = "Creates a new order against an active flash sale item."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Order created.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Validation error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Malformed request or business rule violation.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
    })
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody final CreateOrderDto createOrderDto) {
        final UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Creating order for user {} for flash sale item {}", userId, createOrderDto.flashSaleItemId());
        try {
            final OrderResponseDto response = orderService.createOrder(createOrderDto, userId);
            log.info("Order created successfully: {}", response.orderId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (final Exception e) {
            log.error("Failed to create order", e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }

    /**
     * Get order details by ID.
     *
     * @param orderId
     *            the order ID
     * @return OrderDetailDto with order details or 404 if not found
     */
    @GetMapping("/orders/{orderId}")
    @Operation(
        summary = "Get order by id",
        description = "Returns order details for the authenticated user."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Order found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderDetailDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid UUID format.",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Order not found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
    })
    public ResponseEntity<OrderDetailDto> getOrderById(
        @Parameter(description = "Order identifier (UUID).", example = "2b8efb9f-6f89-4b2d-8c73-4b2f9d4d2e1a")
        @PathVariable final String orderId) {
        final UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Fetching order {} for user {}", orderId, userId);
        try {
            final UUID orderUuid = UUID.fromString(orderId);
            final OrderDetailDto orderDetail = orderService.getOrderById(orderUuid, userId);
            log.info("Fetched order {} for user {}", orderId, userId);
            return ResponseEntity.ok(orderDetail);
        } catch (final IllegalArgumentException e) {
            log.error("Invalid UUID format: orderId={}", orderId);
            return ResponseEntity.badRequest().build();
        } catch (final Exception e) {
            log.error("Failed to fetch order {} for user {}", orderId, userId, e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }

    /**
     * Get user's order history with optional filters.
     *
     * @param status
     *            optional status filter (PENDING, PAID, FAILED, REFUNDED, DISPATCHED)
     * @param startDate
     *            optional start date filter (ISO-8601 format)
     * @param endDate
     *            optional end date filter (ISO-8601 format)
     * @return List of OrderDetailDto matching the criteria
     */
    @GetMapping("/orders")
    @Operation(
        summary = "List orders for a user",
        description = "Returns the authenticated user's order history with optional status and date range filters."
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
        @ApiResponse(
            responseCode = "400",
            description = "Invalid parameters (status/date range).",
            content = @Content
        )
    })
    public ResponseEntity<List<OrderDetailDto>> getOrders(
        @Parameter(description = "Optional status filter.", schema = @Schema(implementation = OrderStatus.class), example = "PAID")
        @RequestParam(required = false) final String status,
        @Parameter(description = "Optional start date/time filter (ISO-8601).", example = "2026-01-01T00:00:00Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime startDate,
        @Parameter(description = "Optional end date/time filter (ISO-8601).", example = "2026-12-31T23:59:59Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime endDate) {
        final UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Fetching orders for user {} with filters: status={}, startDate={}, endDate={}",
            userId, status, startDate, endDate);
        try {
            OrderStatus orderStatus = null;
            if (status != null && !status.isEmpty()) {
                try {
                    orderStatus = OrderStatus.valueOf(status.toUpperCase());
                } catch (final IllegalArgumentException e) {
                    log.warn("Invalid order status: {}", status);
                    return ResponseEntity.badRequest().build();
                }
            }

            final List<OrderDetailDto> orders = orderService.getOrdersByUser(userId, orderStatus, startDate, endDate);
            log.info("Fetched {} orders for user {}", orders.size(), userId);
            return ResponseEntity.ok(orders);
        } catch (final IllegalArgumentException e) {
            log.error("Invalid date range: error={}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (final Exception e) {
            log.error("Failed to fetch orders for user {}", userId, e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }

    /**
     * Request a refund for a PAID order.
     *
     * @param orderId
     *            the order ID
     * @return ResponseEntity with success message
     */
    @PostMapping("/orders/{orderId}/refund")
    @Operation(
        summary = "Request refund",
        description = "Requests a refund for a PAID order."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Refund request accepted/processed.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid order ID format or invalid state for refund.",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Order not found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
    })
    public ResponseEntity<OrderResponseDto> refundOrder(
        @Parameter(description = "Order identifier (UUID).", example = "2b8efb9f-6f89-4b2d-8c73-4b2f9d4d2e1a")
        @PathVariable final String orderId) {
        final UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Processing refund request for order {} by user {}", orderId, userId);
        try {
            final UUID orderUuid = UUID.fromString(orderId);
            // Validate ownership before processing refund
            orderService.getOrderById(orderUuid, userId);
            orderService.handleRefund(orderUuid);
            log.info("Refund processed successfully for order {}", orderId);
            return ResponseEntity.ok(new OrderResponseDto(orderUuid, null, "Refund processed successfully"));
        } catch (final IllegalArgumentException e) {
            log.error("Invalid order ID format: {}", orderId);
            return ResponseEntity.badRequest().build();
        } catch (final Exception e) {
            log.error("Failed to process refund for order {}", orderId, e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }

}
