package uk.co.aosd.flash.controllers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.co.aosd.flash.dto.ClientActiveSaleDto;
import uk.co.aosd.flash.dto.ClientDraftSaleDto;
import uk.co.aosd.flash.dto.ClientProductDto;
import uk.co.aosd.flash.dto.CreateOrderDto;
import uk.co.aosd.flash.dto.OrderResponseDto;
import uk.co.aosd.flash.dto.ProductDto;
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
    public ResponseEntity<Optional<ClientProductDto>> getProductById(@PathVariable final String id) {
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
    public ResponseEntity<List<ClientDraftSaleDto>> getDraftSales(@PathVariable final int days) {
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
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody final CreateOrderDto createOrderDto) {
        log.info("Creating order for user {} for flash sale item {}", createOrderDto.userId(), createOrderDto.flashSaleItemId());
        try {
            final OrderResponseDto response = orderService.createOrder(createOrderDto);
            log.info("Order created successfully: {}", response.orderId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (final Exception e) {
            log.error("Failed to create order", e);
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
    public ResponseEntity<OrderResponseDto> refundOrder(@PathVariable final String orderId) {
        log.info("Processing refund request for order {}", orderId);
        try {
            final UUID orderUuid = UUID.fromString(orderId);
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
