package uk.co.aosd.flash.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import uk.co.aosd.flash.config.TestSecurityConfig;
import uk.co.aosd.flash.domain.FlashSale;
import uk.co.aosd.flash.domain.FlashSaleItem;
import uk.co.aosd.flash.domain.OrderStatus;
import uk.co.aosd.flash.domain.Product;
import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.CreateOrderDto;
import uk.co.aosd.flash.dto.CreateSaleDto;
import uk.co.aosd.flash.dto.OrderResponseDto;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.dto.SaleProductDto;
import uk.co.aosd.flash.repository.FlashSaleItemRepository;
import uk.co.aosd.flash.repository.FlashSaleRepository;
import uk.co.aosd.flash.repository.OrderRepository;
import uk.co.aosd.flash.repository.ProductRepository;

/**
 * Bulk order consistency test that fires many concurrent orders and verifies
 * database integrity.
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = TestSecurityConfig.class)
@Testcontainers
@EnableCaching
@ActiveProfiles({ "test", "admin-service", "api-service" })
@TestPropertySource(properties = { "app.payment.success-rate=0.85", "app.payment.always-succeed=false", "app.payment.always-fail=false" })
public class BulkOrderConsistencyTest {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:latest");

    @Container
    @ServiceConnection(name = "redis")
    @SuppressWarnings("resource")
    public static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:latest"))
        .withExposedPorts(6379);

    @Container
    @ServiceConnection
    public static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq:latest"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private FlashSaleRepository flashSaleRepository;

    @Autowired
    private FlashSaleItemRepository flashSaleItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static ObjectMapper objectMapper;
    private static final Random random = new Random();

    // Test data
    private List<Product> testProducts;
    private FlashSale testSale;
    private List<FlashSaleItem> testSaleItems;
    private Map<UUID, Integer> initialProductStock;
    private Map<UUID, Integer> initialSaleItemSoldCount;

    @BeforeAll
    public static void beforeAll() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    public void setUp() throws Exception {
        // Clean up any existing data
        orderRepository.deleteAll();
        flashSaleItemRepository.deleteAll();
        flashSaleRepository.deleteAll();
        productRepository.deleteAll();

        // Create test products with high physical stock
        testProducts = new ArrayList<>();
        initialProductStock = new HashMap<>();

        for (int i = 1; i <= 5; i++) {
            final ProductDto productDto = new ProductDto(
                null,
                "Test Product " + i,
                "Description for product " + i,
                1000, // High physical stock
                BigDecimal.valueOf(99.99 + i),
                0 // No reserved count initially
            );

            final var response = mockMvc.perform(
                post("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productDto)))
                .andExpect(status().isCreated())
                .andReturn();

            final String productUri = response.getResponse().getHeader(HttpHeaders.LOCATION);
            final var productResult = mockMvc.perform(
                get(productUri))
                .andReturn();
            final ProductDto savedProduct = objectMapper.readValue(productResult.getResponse().getContentAsString(), ProductDto.class);

            final Product product = productRepository.findById(UUID.fromString(savedProduct.id())).orElseThrow();
            testProducts.add(product);
            initialProductStock.put(product.getId(), product.getTotalPhysicalStock());
        }

        // Create an active flash sale with all products
        final OffsetDateTime now = OffsetDateTime.now();
        final OffsetDateTime startTime = now.minusHours(1); // Started in the past
        final OffsetDateTime endTime = now.plusHours(2); // Ends in the future

        final List<SaleProductDto> saleProducts = testProducts.stream()
            .map(p -> new SaleProductDto(p.getId().toString(), 100)) // 100 units allocated per item
            .collect(Collectors.toList());

        final CreateSaleDto saleDto = new CreateSaleDto(
            null,
            "Bulk Test Sale",
            startTime,
            endTime,
            SaleStatus.ACTIVE, // Create as ACTIVE directly
            saleProducts);

        final var saleResponse = mockMvc.perform(
            post("/api/v1/admin/flash_sale")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saleDto)))
            .andExpect(status().isCreated())
            .andReturn();

        final String saleUri = saleResponse.getResponse().getHeader(HttpHeaders.LOCATION);
        final String saleIdStr = saleUri.substring("/api/v1/admin/flash_sale/".length());
        final UUID saleId = UUID.fromString(saleIdStr);
        testSale = flashSaleRepository.findById(saleId).orElseThrow();

        // Get all sale items for this sale
        testSaleItems = flashSaleItemRepository.findAll().stream()
            .filter(item -> item.getFlashSale().getId().equals(saleId))
            .collect(Collectors.toList());

        // Capture initial sold counts
        initialSaleItemSoldCount = new HashMap<>();
        for (final FlashSaleItem item : testSaleItems) {
            initialSaleItemSoldCount.put(item.getId(), item.getSoldCount());
        }

        assertEquals(5, testProducts.size());
        assertEquals(5, testSaleItems.size());
    }

    @Test
    public void shouldMaintainDatabaseConsistencyUnderBulkOrderLoad() throws Exception {
        // Fire many concurrent orders
        final int totalOrders = 800;
        final int threadPoolSize = 50;
        final ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        final List<CompletableFuture<OrderResult>> futures = new ArrayList<>();

        final long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalOrders; i++) {
            final CompletableFuture<OrderResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    final UUID userId = UUID.randomUUID();
                    final FlashSaleItem randomItem = testSaleItems.get(random.nextInt(testSaleItems.size()));
                    final int quantity = 1 + random.nextInt(5); // Random quantity 1-5

                    final CreateOrderDto orderDto = new CreateOrderDto(randomItem.getId(), quantity);

                    final var result = mockMvc.perform(
                        post("/api/v1/clients/orders")
                            .with(SecurityMockMvcRequestPostProcessors.authentication(
                                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                    userId, null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(orderDto)))
                        .andReturn();

                    if (result.getResponse().getStatus() == 201) {
                        final String responseBody = result.getResponse().getContentAsString();
                        final OrderResponseDto response = objectMapper.readValue(responseBody, OrderResponseDto.class);
                        return new OrderResult(true, (String) null, (Integer) null, response.orderId());
                    } else {
                        return new OrderResult(false, (String) null, result.getResponse().getStatus(), (UUID) null);
                    }
                } catch (final Exception e) {
                    return new OrderResult(false, e.getMessage(), (Integer) null, (UUID) null);
                }
            }, executor);

            futures.add(future);
        }

        // Wait for all order creation attempts to complete
        final List<OrderResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        final long orderCreationTime = System.currentTimeMillis() - startTime;
        final long successfulOrders = results.stream().filter(r -> r.success).count();
        final long failedOrders = results.stream().filter(r -> !r.success).count();

        System.out.println("Order creation completed in " + orderCreationTime + "ms");
        System.out.println("Successful orders: " + successfulOrders);
        System.out.println("Failed orders: " + failedOrders);

        // Wait for async processing to complete
        waitForAsyncProcessing(60);

        // Give a bit more time for any final database updates to flush
        Thread.sleep(2000);

        // Verify consistency
        verifyNoOversoldStock();
        verifyNoUnfulfillablePaidOrders();
        verifyProductStockCorrectlyUpdated();
        verifySaleItemSoldCountMatchesOrders();
        verifyAdditionalConstraints();
    }

    private void waitForAsyncProcessing(final int timeoutSeconds) throws InterruptedException {
        final long startTime = System.currentTimeMillis();
        final long timeoutMs = timeoutSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            // Check if all orders are in terminal states (no PENDING or PAID orders)
            final long pendingCount = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .count();

            final long paidCount = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .count();

            if (pendingCount == 0 && paidCount == 0) {
                System.out.println("All orders processed after " + (System.currentTimeMillis() - startTime) + "ms");
                return;
            }

            Thread.sleep(500); // Poll every 500ms
        }

        // Final check
        final long pendingCount = orderRepository.findAll().stream()
            .filter(o -> o.getStatus() == OrderStatus.PENDING)
            .count();

        final long paidCount = orderRepository.findAll().stream()
            .filter(o -> o.getStatus() == OrderStatus.PAID)
            .count();

        if (pendingCount > 0 || paidCount > 0) {
            System.out.println("Warning: " + pendingCount + " orders still in PENDING state and " + paidCount + " orders still in PAID state after timeout");
        }
    }

    private void verifyNoOversoldStock() {
        final String sql = """
            SELECT fsi.id, fsi.allocated_stock, fsi.sold_count,
                   (fsi.sold_count - fsi.allocated_stock) as oversold
            FROM flash_sale_items fsi
            WHERE fsi.sold_count > fsi.allocated_stock
            """;

        final List<Map<String, Object>> oversoldItems = jdbcTemplate.queryForList(sql);

        assertTrue(oversoldItems.isEmpty(),
            "Found oversold stock: " + oversoldItems.stream()
                .map(m -> "Item " + m.get("id") + ": sold=" + m.get("sold_count") + ", allocated=" + m.get("allocated_stock"))
                .collect(Collectors.joining(", ")));
    }

    private void verifyNoUnfulfillablePaidOrders() {
        final String sql = """
            SELECT o.id, o.status, o.sold_quantity, fsi.allocated_stock, fsi.sold_count,
                   (fsi.allocated_stock - fsi.sold_count) as available_stock
            FROM orders o
            JOIN flash_sale_items fsi ON o.flash_sale_item_id = fsi.id
            WHERE o.status = 'PAID'
              AND o.sold_quantity > (fsi.allocated_stock - fsi.sold_count)
            """;

        final List<Map<String, Object>> unfulfillableOrders = jdbcTemplate.queryForList(sql);

        assertTrue(unfulfillableOrders.isEmpty(),
            "Found unfulfillable PAID orders: " + unfulfillableOrders.stream()
                .map(m -> "Order " + m.get("id") + ": quantity=" + m.get("sold_quantity") + ", available=" + m.get("available_stock"))
                .collect(Collectors.joining(", ")));
    }

    private void verifyProductStockCorrectlyUpdated() {
        // For each product, verify that total_physical_stock = initial_stock - sum of
        // dispatched quantities
        for (final Product product : testProducts) {
            final UUID productId = product.getId();
            final Integer initialStock = initialProductStock.get(productId);

            final String sql = """
                SELECT COALESCE(SUM(o.sold_quantity), 0) as total_dispatched
                FROM orders o
                WHERE o.product_id = ? AND o.status = 'DISPATCHED'
                """;

            final Integer totalDispatched = jdbcTemplate.queryForObject(sql, Integer.class, productId);

            // Refresh the product entity to ensure we have the latest data
            productRepository.flush();
            final Product currentProduct = productRepository.findById(productId).orElseThrow();

            final Integer expectedStock = initialStock - totalDispatched;
            assertEquals(expectedStock, currentProduct.getTotalPhysicalStock(),
                "Product " + productId + " stock mismatch. Expected: " + expectedStock + " (initial: " + initialStock + " - dispatched: " + totalDispatched
                    + "), Actual: " + currentProduct.getTotalPhysicalStock());
        }
    }

    private void verifySaleItemSoldCountMatchesOrders() {
        final String sql = """
            SELECT fsi.id, fsi.sold_count,
                   COALESCE(SUM(CASE WHEN o.status IN ('PENDING', 'PAID', 'DISPATCHED')
                                THEN o.sold_quantity ELSE 0 END), 0) as order_quantity_sum
            FROM flash_sale_items fsi
            LEFT JOIN orders o ON o.flash_sale_item_id = fsi.id
            WHERE fsi.flash_sale_id = ?
            GROUP BY fsi.id, fsi.sold_count
            HAVING fsi.sold_count != COALESCE(SUM(CASE WHEN o.status IN ('PENDING', 'PAID', 'DISPATCHED')
                                                  THEN o.sold_quantity ELSE 0 END), 0)
            """;

        final List<Map<String, Object>> mismatches = jdbcTemplate.queryForList(sql, testSale.getId());

        assertTrue(mismatches.isEmpty(),
            "Found sold_count mismatches: " + mismatches.stream()
                .map(m -> "Item " + m.get("id") + ": sold_count=" + m.get("sold_count") + ", order_sum=" + m.get("order_quantity_sum"))
                .collect(Collectors.joining(", ")));
    }

    private void verifyAdditionalConstraints() {
        // Verify all sale items have sold_count <= allocated_stock
        for (final FlashSaleItem item : testSaleItems) {
            final FlashSaleItem currentItem = flashSaleItemRepository.findById(item.getId()).orElseThrow();
            assertTrue(currentItem.getSoldCount() <= currentItem.getAllocatedStock(),
                "Sale item " + item.getId() + " has sold_count (" + currentItem.getSoldCount() + ") > allocated_stock (" + currentItem.getAllocatedStock()
                    + ")");
        }

        // Verify all products have non-negative stock
        for (final Product product : testProducts) {
            final Product currentProduct = productRepository.findById(product.getId()).orElseThrow();
            assertTrue(currentProduct.getTotalPhysicalStock() >= 0,
                "Product " + product.getId() + " has negative stock: " + currentProduct.getTotalPhysicalStock());
        }
    }

    private static class OrderResult {
        final boolean success;

        OrderResult(final boolean success, final String errorMessage, final Integer statusCode, final UUID orderId) {
            this.success = success;
        }
    }
}
