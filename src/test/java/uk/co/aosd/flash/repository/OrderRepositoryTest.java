package uk.co.aosd.flash.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;
import uk.co.aosd.flash.domain.FlashSale;
import uk.co.aosd.flash.domain.FlashSaleItem;
import uk.co.aosd.flash.domain.Order;
import uk.co.aosd.flash.domain.OrderStatus;
import uk.co.aosd.flash.domain.Product;
import uk.co.aosd.flash.domain.SaleStatus;

/**
 * Test the Order Repository.
 */
@DataJpaTest
public class OrderRepositoryTest {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private FlashSaleRepository flashSaleRepository;

    @Autowired
    private FlashSaleItemRepository flashSaleItemRepository;

    @Test
    public void shouldFindOrderByIdAndUserId() {
        final UUID userId1 = UUID.randomUUID();
        final UUID userId2 = UUID.randomUUID();

        // Create test data
        final Product product = new Product(null, "Test Product", "Description", 100, BigDecimal.valueOf(99.99), 10);
        final Product savedProduct = productRepository.save(product);

        final FlashSale flashSale = new FlashSale(null, "Test Sale",
            OffsetDateTime.now().minusHours(1),
            OffsetDateTime.now().plusHours(1),
            SaleStatus.ACTIVE, List.of());
        final FlashSale savedFlashSale = flashSaleRepository.save(flashSale);

        final FlashSaleItem flashSaleItem = new FlashSaleItem(null, savedFlashSale, savedProduct, 50, 0, BigDecimal.valueOf(79.99));
        final FlashSaleItem savedFlashSaleItem = flashSaleItemRepository.save(flashSaleItem);

        final Order order1 = new Order();
        order1.setUserId(userId1);
        order1.setFlashSaleItem(savedFlashSaleItem);
        order1.setProduct(savedProduct);
        order1.setSoldPrice(BigDecimal.valueOf(79.99));
        order1.setSoldQuantity(5);
        order1.setStatus(OrderStatus.PAID);
        order1.setCreatedAt(OffsetDateTime.now());
        final Order savedOrder1 = orderRepository.save(order1);

        final Order order2 = new Order();
        order2.setUserId(userId2);
        order2.setFlashSaleItem(savedFlashSaleItem);
        order2.setProduct(savedProduct);
        order2.setSoldPrice(BigDecimal.valueOf(79.99));
        order2.setSoldQuantity(3);
        order2.setStatus(OrderStatus.PENDING);
        order2.setCreatedAt(OffsetDateTime.now());
        orderRepository.save(order2);

        // Test finding order by ID and userId
        final Optional<Order> found = orderRepository.findByIdAndUserId(savedOrder1.getId(), userId1);
        assertTrue(found.isPresent());
        assertEquals(savedOrder1.getId(), found.get().getId());
        assertEquals(userId1, found.get().getUserId());

        // Test that order belongs to different user is not found
        final Optional<Order> notFound = orderRepository.findByIdAndUserId(savedOrder1.getId(), userId2);
        assertFalse(notFound.isPresent());
    }

    @Test
    public void shouldFindOrdersByUserId() {
        final UUID userId1 = UUID.randomUUID();
        final UUID userId2 = UUID.randomUUID();

        // Create test data
        final Product product = new Product(null, "Test Product", "Description", 100, BigDecimal.valueOf(99.99), 10);
        final Product savedProduct = productRepository.save(product);

        final FlashSale flashSale1 = new FlashSale(null, "Test Sale 1",
            OffsetDateTime.now().minusHours(1),
            OffsetDateTime.now().plusHours(1),
            SaleStatus.ACTIVE, List.of());
        final FlashSale savedFlashSale1 = flashSaleRepository.save(flashSale1);

        final FlashSale flashSale2 = new FlashSale(null, "Test Sale 2",
            OffsetDateTime.now().minusHours(1),
            OffsetDateTime.now().plusHours(1),
            SaleStatus.ACTIVE, List.of());
        final FlashSale savedFlashSale2 = flashSaleRepository.save(flashSale2);

        final FlashSaleItem flashSaleItem1 = new FlashSaleItem(null, savedFlashSale1, savedProduct, 50, 0, BigDecimal.valueOf(79.99));
        final FlashSaleItem savedFlashSaleItem1 = flashSaleItemRepository.save(flashSaleItem1);

        final FlashSaleItem flashSaleItem2 = new FlashSaleItem(null, savedFlashSale2, savedProduct, 50, 0, BigDecimal.valueOf(89.99));
        final FlashSaleItem savedFlashSaleItem2 = flashSaleItemRepository.save(flashSaleItem2);

        // Create orders for user1 (using different flash sale items to avoid unique constraint)
        final Order order1 = new Order();
        order1.setUserId(userId1);
        order1.setFlashSaleItem(savedFlashSaleItem1);
        order1.setProduct(savedProduct);
        order1.setSoldPrice(BigDecimal.valueOf(79.99));
        order1.setSoldQuantity(5);
        order1.setStatus(OrderStatus.PAID);
        order1.setCreatedAt(OffsetDateTime.now().minusDays(1));
        orderRepository.save(order1);

        final Order order2 = new Order();
        order2.setUserId(userId1);
        order2.setFlashSaleItem(savedFlashSaleItem2);
        order2.setProduct(savedProduct);
        order2.setSoldPrice(BigDecimal.valueOf(89.99));
        order2.setSoldQuantity(3);
        order2.setStatus(OrderStatus.PENDING);
        order2.setCreatedAt(OffsetDateTime.now());
        orderRepository.save(order2);

        // Create order for user2
        final Order order3 = new Order();
        order3.setUserId(userId2);
        order3.setFlashSaleItem(savedFlashSaleItem1);
        order3.setProduct(savedProduct);
        order3.setSoldPrice(BigDecimal.valueOf(99.99));
        order3.setSoldQuantity(2);
        order3.setStatus(OrderStatus.PAID);
        order3.setCreatedAt(OffsetDateTime.now());
        orderRepository.save(order3);

        // Test finding orders by userId
        final List<Order> user1Orders = orderRepository.findByUserId(userId1);
        assertEquals(2, user1Orders.size());
        // Should be ordered by createdAt DESC (most recent first)
        assertTrue(user1Orders.get(0).getCreatedAt().isAfter(user1Orders.get(1).getCreatedAt()) ||
            user1Orders.get(0).getCreatedAt().isEqual(user1Orders.get(1).getCreatedAt()));

        final List<Order> user2Orders = orderRepository.findByUserId(userId2);
        assertEquals(1, user2Orders.size());
    }

    @Test
    public void shouldFindOrdersByUserIdAndStatus() {
        final UUID userId = UUID.randomUUID();

        // Create test data
        final Product product = new Product(null, "Test Product", "Description", 100, BigDecimal.valueOf(99.99), 10);
        final Product savedProduct = productRepository.save(product);

        final FlashSale flashSale1 = new FlashSale(null, "Test Sale 1",
            OffsetDateTime.now().minusHours(1),
            OffsetDateTime.now().plusHours(1),
            SaleStatus.ACTIVE, List.of());
        final FlashSale savedFlashSale1 = flashSaleRepository.save(flashSale1);

        final FlashSale flashSale2 = new FlashSale(null, "Test Sale 2",
            OffsetDateTime.now().minusHours(1),
            OffsetDateTime.now().plusHours(1),
            SaleStatus.ACTIVE, List.of());
        final FlashSale savedFlashSale2 = flashSaleRepository.save(flashSale2);

        final FlashSale flashSale3 = new FlashSale(null, "Test Sale 3",
            OffsetDateTime.now().minusHours(1),
            OffsetDateTime.now().plusHours(1),
            SaleStatus.ACTIVE, List.of());
        final FlashSale savedFlashSale3 = flashSaleRepository.save(flashSale3);

        final FlashSaleItem flashSaleItem1 = new FlashSaleItem(null, savedFlashSale1, savedProduct, 50, 0, BigDecimal.valueOf(79.99));
        final FlashSaleItem savedFlashSaleItem1 = flashSaleItemRepository.save(flashSaleItem1);

        final FlashSaleItem flashSaleItem2 = new FlashSaleItem(null, savedFlashSale2, savedProduct, 50, 0, BigDecimal.valueOf(89.99));
        final FlashSaleItem savedFlashSaleItem2 = flashSaleItemRepository.save(flashSaleItem2);

        final FlashSaleItem flashSaleItem3 = new FlashSaleItem(null, savedFlashSale3, savedProduct, 50, 0, BigDecimal.valueOf(99.99));
        final FlashSaleItem savedFlashSaleItem3 = flashSaleItemRepository.save(flashSaleItem3);

        // Create orders with different statuses (using different flash sale items)
        final Order order1 = new Order();
        order1.setUserId(userId);
        order1.setFlashSaleItem(savedFlashSaleItem1);
        order1.setProduct(savedProduct);
        order1.setSoldPrice(BigDecimal.valueOf(79.99));
        order1.setSoldQuantity(5);
        order1.setStatus(OrderStatus.PAID);
        order1.setCreatedAt(OffsetDateTime.now());
        orderRepository.save(order1);

        final Order order2 = new Order();
        order2.setUserId(userId);
        order2.setFlashSaleItem(savedFlashSaleItem2);
        order2.setProduct(savedProduct);
        order2.setSoldPrice(BigDecimal.valueOf(89.99));
        order2.setSoldQuantity(3);
        order2.setStatus(OrderStatus.PENDING);
        order2.setCreatedAt(OffsetDateTime.now());
        orderRepository.save(order2);

        final Order order3 = new Order();
        order3.setUserId(userId);
        order3.setFlashSaleItem(savedFlashSaleItem3);
        order3.setProduct(savedProduct);
        order3.setSoldPrice(BigDecimal.valueOf(99.99));
        order3.setSoldQuantity(2);
        order3.setStatus(OrderStatus.PAID);
        order3.setCreatedAt(OffsetDateTime.now());
        orderRepository.save(order3);

        // Test finding orders by userId and status
        final List<Order> paidOrders = orderRepository.findByUserIdAndStatus(userId, OrderStatus.PAID);
        assertEquals(2, paidOrders.size());
        paidOrders.forEach(order -> assertEquals(OrderStatus.PAID, order.getStatus()));

        final List<Order> pendingOrders = orderRepository.findByUserIdAndStatus(userId, OrderStatus.PENDING);
        assertEquals(1, pendingOrders.size());
        assertEquals(OrderStatus.PENDING, pendingOrders.get(0).getStatus());
    }

    @Test
    public void shouldFindOrdersByUserIdAndDateRange() {
        final UUID userId = UUID.randomUUID();
        final OffsetDateTime baseTime = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime startDate = baseTime.minusDays(5);
        final OffsetDateTime endDate = baseTime.plusDays(5);

        // Create test data
        final Product product = new Product(null, "Test Product", "Description", 100, BigDecimal.valueOf(99.99), 10);
        final Product savedProduct = productRepository.save(product);

        final FlashSale flashSale1 = new FlashSale(null, "Test Sale 1",
            OffsetDateTime.now().minusHours(1),
            OffsetDateTime.now().plusHours(1),
            SaleStatus.ACTIVE, List.of());
        final FlashSale savedFlashSale1 = flashSaleRepository.save(flashSale1);

        final FlashSale flashSale2 = new FlashSale(null, "Test Sale 2",
            OffsetDateTime.now().minusHours(1),
            OffsetDateTime.now().plusHours(1),
            SaleStatus.ACTIVE, List.of());
        final FlashSale savedFlashSale2 = flashSaleRepository.save(flashSale2);

        final FlashSale flashSale3 = new FlashSale(null, "Test Sale 3",
            OffsetDateTime.now().minusHours(1),
            OffsetDateTime.now().plusHours(1),
            SaleStatus.ACTIVE, List.of());
        final FlashSale savedFlashSale3 = flashSaleRepository.save(flashSale3);

        final FlashSaleItem flashSaleItem1 = new FlashSaleItem(null, savedFlashSale1, savedProduct, 50, 0, BigDecimal.valueOf(79.99));
        final FlashSaleItem savedFlashSaleItem1 = flashSaleItemRepository.save(flashSaleItem1);

        final FlashSaleItem flashSaleItem2 = new FlashSaleItem(null, savedFlashSale2, savedProduct, 50, 0, BigDecimal.valueOf(89.99));
        final FlashSaleItem savedFlashSaleItem2 = flashSaleItemRepository.save(flashSaleItem2);

        final FlashSaleItem flashSaleItem3 = new FlashSaleItem(null, savedFlashSale3, savedProduct, 50, 0, BigDecimal.valueOf(99.99));
        final FlashSaleItem savedFlashSaleItem3 = flashSaleItemRepository.save(flashSaleItem3);

        // Create orders within date range
        final Order order1 = new Order();
        order1.setUserId(userId);
        order1.setFlashSaleItem(savedFlashSaleItem1);
        order1.setProduct(savedProduct);
        order1.setSoldPrice(BigDecimal.valueOf(79.99));
        order1.setSoldQuantity(5);
        order1.setStatus(OrderStatus.PAID);
        order1.setCreatedAt(baseTime.minusDays(3));
        orderRepository.save(order1);

        final Order order2 = new Order();
        order2.setUserId(userId);
        order2.setFlashSaleItem(savedFlashSaleItem2);
        order2.setProduct(savedProduct);
        order2.setSoldPrice(BigDecimal.valueOf(89.99));
        order2.setSoldQuantity(3);
        order2.setStatus(OrderStatus.PENDING);
        order2.setCreatedAt(baseTime);
        orderRepository.save(order2);

        // Create order outside date range
        final Order order3 = new Order();
        order3.setUserId(userId);
        order3.setFlashSaleItem(savedFlashSaleItem3);
        order3.setProduct(savedProduct);
        order3.setSoldPrice(BigDecimal.valueOf(99.99));
        order3.setSoldQuantity(2);
        order3.setStatus(OrderStatus.PAID);
        order3.setCreatedAt(baseTime.minusDays(10)); // Before startDate
        orderRepository.save(order3);

        // Test finding orders by userId and date range
        final List<Order> ordersInRange = orderRepository.findByUserIdAndCreatedAtBetween(userId, startDate, endDate);
        assertEquals(2, ordersInRange.size());
        ordersInRange.forEach(order -> {
            assertTrue(order.getCreatedAt().isAfter(startDate.minusSeconds(1)) || order.getCreatedAt().isEqual(startDate));
            assertTrue(order.getCreatedAt().isBefore(endDate.plusSeconds(1)) || order.getCreatedAt().isEqual(endDate));
        });
    }

    @Test
    public void shouldFindOrdersByUserIdAndStatusAndDateRange() {
        final UUID userId = UUID.randomUUID();
        final OffsetDateTime baseTime = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime startDate = baseTime.minusDays(5);
        final OffsetDateTime endDate = baseTime.plusDays(5);

        // Create test data
        final Product product = new Product(null, "Test Product", "Description", 100, BigDecimal.valueOf(99.99), 10);
        final Product savedProduct = productRepository.save(product);

        final FlashSale flashSale1 = new FlashSale(null, "Test Sale 1",
            OffsetDateTime.now().minusHours(1),
            OffsetDateTime.now().plusHours(1),
            SaleStatus.ACTIVE, List.of());
        final FlashSale savedFlashSale1 = flashSaleRepository.save(flashSale1);

        final FlashSale flashSale2 = new FlashSale(null, "Test Sale 2",
            OffsetDateTime.now().minusHours(1),
            OffsetDateTime.now().plusHours(1),
            SaleStatus.ACTIVE, List.of());
        final FlashSale savedFlashSale2 = flashSaleRepository.save(flashSale2);

        final FlashSale flashSale3 = new FlashSale(null, "Test Sale 3",
            OffsetDateTime.now().minusHours(1),
            OffsetDateTime.now().plusHours(1),
            SaleStatus.ACTIVE, List.of());
        final FlashSale savedFlashSale3 = flashSaleRepository.save(flashSale3);

        final FlashSale flashSale4 = new FlashSale(null, "Test Sale 4",
            OffsetDateTime.now().minusHours(1),
            OffsetDateTime.now().plusHours(1),
            SaleStatus.ACTIVE, List.of());
        final FlashSale savedFlashSale4 = flashSaleRepository.save(flashSale4);

        final FlashSaleItem flashSaleItem1 = new FlashSaleItem(null, savedFlashSale1, savedProduct, 50, 0, BigDecimal.valueOf(79.99));
        final FlashSaleItem savedFlashSaleItem1 = flashSaleItemRepository.save(flashSaleItem1);

        final FlashSaleItem flashSaleItem2 = new FlashSaleItem(null, savedFlashSale2, savedProduct, 50, 0, BigDecimal.valueOf(89.99));
        final FlashSaleItem savedFlashSaleItem2 = flashSaleItemRepository.save(flashSaleItem2);

        final FlashSaleItem flashSaleItem3 = new FlashSaleItem(null, savedFlashSale3, savedProduct, 50, 0, BigDecimal.valueOf(99.99));
        final FlashSaleItem savedFlashSaleItem3 = flashSaleItemRepository.save(flashSaleItem3);

        final FlashSaleItem flashSaleItem4 = new FlashSaleItem(null, savedFlashSale4, savedProduct, 50, 0, BigDecimal.valueOf(109.99));
        final FlashSaleItem savedFlashSaleItem4 = flashSaleItemRepository.save(flashSaleItem4);

        // Create PAID orders within date range
        final Order order1 = new Order();
        order1.setUserId(userId);
        order1.setFlashSaleItem(savedFlashSaleItem1);
        order1.setProduct(savedProduct);
        order1.setSoldPrice(BigDecimal.valueOf(79.99));
        order1.setSoldQuantity(5);
        order1.setStatus(OrderStatus.PAID);
        order1.setCreatedAt(baseTime.minusDays(3));
        orderRepository.save(order1);

        final Order order2 = new Order();
        order2.setUserId(userId);
        order2.setFlashSaleItem(savedFlashSaleItem2);
        order2.setProduct(savedProduct);
        order2.setSoldPrice(BigDecimal.valueOf(89.99));
        order2.setSoldQuantity(3);
        order2.setStatus(OrderStatus.PAID);
        order2.setCreatedAt(baseTime);
        orderRepository.save(order2);

        // Create PENDING order within date range (should not be included)
        final Order order3 = new Order();
        order3.setUserId(userId);
        order3.setFlashSaleItem(savedFlashSaleItem3);
        order3.setProduct(savedProduct);
        order3.setSoldPrice(BigDecimal.valueOf(99.99));
        order3.setSoldQuantity(2);
        order3.setStatus(OrderStatus.PENDING);
        order3.setCreatedAt(baseTime.minusDays(2));
        orderRepository.save(order3);

        // Create PAID order outside date range (should not be included)
        final Order order4 = new Order();
        order4.setUserId(userId);
        order4.setFlashSaleItem(savedFlashSaleItem4);
        order4.setProduct(savedProduct);
        order4.setSoldPrice(BigDecimal.valueOf(109.99));
        order4.setSoldQuantity(2);
        order4.setStatus(OrderStatus.PAID);
        order4.setCreatedAt(baseTime.minusDays(10));
        orderRepository.save(order4);

        // Test finding orders by userId, status, and date range
        final List<Order> paidOrdersInRange = orderRepository.findByUserIdAndStatusAndCreatedAtBetween(
            userId, OrderStatus.PAID, startDate, endDate);
        assertEquals(2, paidOrdersInRange.size());
        paidOrdersInRange.forEach(order -> {
            assertEquals(OrderStatus.PAID, order.getStatus());
            assertTrue(order.getCreatedAt().isAfter(startDate.minusSeconds(1)) || order.getCreatedAt().isEqual(startDate));
            assertTrue(order.getCreatedAt().isBefore(endDate.plusSeconds(1)) || order.getCreatedAt().isEqual(endDate));
        });
    }

    @Test
    public void shouldEagerlyLoadRelatedEntities() {
        final UUID userId = UUID.randomUUID();

        // Create test data
        final Product product = new Product(null, "Test Product", "Description", 100, BigDecimal.valueOf(99.99), 10);
        final Product savedProduct = productRepository.save(product);

        final FlashSale flashSale = new FlashSale(null, "Test Sale",
            OffsetDateTime.now().minusHours(1),
            OffsetDateTime.now().plusHours(1),
            SaleStatus.ACTIVE, List.of());
        final FlashSale savedFlashSale = flashSaleRepository.save(flashSale);

        final FlashSaleItem flashSaleItem = new FlashSaleItem(null, savedFlashSale, savedProduct, 50, 0, BigDecimal.valueOf(79.99));
        final FlashSaleItem savedFlashSaleItem = flashSaleItemRepository.save(flashSaleItem);

        final Order order = new Order();
        order.setUserId(userId);
        order.setFlashSaleItem(savedFlashSaleItem);
        order.setProduct(savedProduct);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.PAID);
        order.setCreatedAt(OffsetDateTime.now());
        final Order savedOrder = orderRepository.save(order);

        // Test that related entities are eagerly loaded
        final Optional<Order> found = orderRepository.findByIdAndUserId(savedOrder.getId(), userId);
        assertTrue(found.isPresent());
        final Order foundOrder = found.get();

        // These should not trigger lazy loading exceptions
        assertNotNull(foundOrder.getProduct());
        assertEquals("Test Product", foundOrder.getProduct().getName());
        assertNotNull(foundOrder.getFlashSaleItem());
        assertNotNull(foundOrder.getFlashSaleItem().getFlashSale());
        assertEquals("Test Sale", foundOrder.getFlashSaleItem().getFlashSale().getTitle());
    }
}
