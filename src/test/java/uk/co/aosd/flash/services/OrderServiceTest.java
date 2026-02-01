package uk.co.aosd.flash.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.co.aosd.flash.domain.FlashSale;
import uk.co.aosd.flash.domain.FlashSaleItem;
import uk.co.aosd.flash.domain.Order;
import uk.co.aosd.flash.domain.OrderStatusHistory;
import uk.co.aosd.flash.domain.OrderStatus;
import uk.co.aosd.flash.domain.Product;
import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.CreateOrderDto;
import uk.co.aosd.flash.dto.OrderDetailDto;
import uk.co.aosd.flash.dto.OrderResponseDto;
import uk.co.aosd.flash.dto.ProcessPaymentResult;
import uk.co.aosd.flash.exc.InsufficientStockException;
import uk.co.aosd.flash.exc.InvalidOrderStatusException;
import uk.co.aosd.flash.exc.OrderNotFoundException;
import uk.co.aosd.flash.exc.SaleNotActiveException;
import uk.co.aosd.flash.repository.FlashSaleItemRepository;
import uk.co.aosd.flash.repository.OrderRepository;
import uk.co.aosd.flash.repository.OrderStatusHistoryRepository;
import uk.co.aosd.flash.repository.ProductRepository;

/**
 * Test the Order Service.
 */
public class OrderServiceTest {

    private OrderRepository orderRepository;
    private FlashSaleItemRepository flashSaleItemRepository;
    private ProductRepository productRepository;
    private OrderStatusHistoryRepository orderStatusHistoryRepository;
    private AuditLogService auditLogService;
    private PaymentService paymentService;
    private NotificationService notificationService;
    private OrderService orderService;

    private UUID userId;
    private UUID flashSaleItemId;
    private UUID productId;
    private UUID flashSaleId;
    private Product product;
    private FlashSale flashSale;
    private FlashSaleItem flashSaleItem;

    @BeforeEach
    public void beforeEach() {
        orderRepository = Mockito.mock(OrderRepository.class);
        flashSaleItemRepository = Mockito.mock(FlashSaleItemRepository.class);
        productRepository = Mockito.mock(ProductRepository.class);
        orderStatusHistoryRepository = Mockito.mock(OrderStatusHistoryRepository.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        paymentService = Mockito.mock(PaymentService.class);
        notificationService = Mockito.mock(NotificationService.class);

        orderService = new OrderService(
            orderRepository,
            flashSaleItemRepository,
            productRepository,
            orderStatusHistoryRepository,
            auditLogService,
            paymentService,
            notificationService);

        Mockito.when(orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(any())).thenReturn(List.of());

        userId = UUID.randomUUID();
        flashSaleItemId = UUID.randomUUID();
        productId = UUID.randomUUID();
        flashSaleId = UUID.randomUUID();

        product = new Product(productId, "Test Product", "Description", 100, BigDecimal.valueOf(99.99), 10);
        flashSale = new FlashSale(flashSaleId, "Test Sale",
            OffsetDateTime.now().minusHours(1),
            OffsetDateTime.now().plusHours(1),
            SaleStatus.ACTIVE, null);
        flashSaleItem = new FlashSaleItem(flashSaleItemId, flashSale, product, 50, 10, BigDecimal.valueOf(79.99));
    }

    @Test
    public void shouldCreateOrderSuccessfully() {
        final CreateOrderDto createOrderDto = new CreateOrderDto(flashSaleItemId, 5);

        Mockito.when(flashSaleItemRepository.findById(flashSaleItemId)).thenReturn(Optional.of(flashSaleItem));
        Mockito.when(flashSaleItemRepository.incrementSoldCount(flashSaleItemId, 5)).thenReturn(1);
        Mockito.when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            final Order order = invocation.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });

        final OrderResponseDto response = orderService.createOrder(createOrderDto, userId);

        assertNotNull(response);
        assertNotNull(response.orderId());
        assertEquals(OrderStatus.PENDING, response.status());
        Mockito.verify(notificationService).sendOrderConfirmation(eq(userId), eq(response.orderId()));
    }

    @Test
    public void shouldFailWhenSaleHasEnded() {
        final CreateOrderDto createOrderDto = new CreateOrderDto(flashSaleItemId, 5);
        flashSale.setEndTime(OffsetDateTime.now().minusHours(1));

        Mockito.when(flashSaleItemRepository.findById(flashSaleItemId)).thenReturn(Optional.of(flashSaleItem));

        assertThrows(SaleNotActiveException.class, () -> {
            orderService.createOrder(createOrderDto, userId);
        });
    }

    @Test
    public void shouldFailWhenInsufficientStock() {
        final CreateOrderDto createOrderDto = new CreateOrderDto(flashSaleItemId, 50);
        flashSaleItem.setSoldCount(45); // Only 5 available, but requesting 50

        Mockito.when(flashSaleItemRepository.findById(flashSaleItemId)).thenReturn(Optional.of(flashSaleItem));

        assertThrows(InsufficientStockException.class, () -> {
            orderService.createOrder(createOrderDto, userId);
        });
    }

    @Test
    public void shouldProcessPaymentSuccessfully() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.PENDING);

        Mockito.when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(paymentService.processPayment(orderId, BigDecimal.valueOf(399.95))).thenReturn(true);

        final ProcessPaymentResult result = orderService.processOrderPayment(orderId);

        assertNotNull(result);
        assertTrue(result.success());
        assertEquals(orderId, result.orderId());
        assertEquals(OrderStatus.PAID, order.getStatus());
    }

    @Test
    public void shouldProcessPaymentFailure() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.PENDING);

        Mockito.when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(paymentService.processPayment(orderId, BigDecimal.valueOf(399.95))).thenReturn(false);

        final ProcessPaymentResult result = orderService.processOrderPayment(orderId);

        assertNotNull(result);
        assertTrue(!result.success());
        assertEquals(orderId, result.orderId());
        assertEquals(OrderStatus.FAILED, order.getStatus());
    }

    @Test
    public void shouldHandleRefundSuccessfully() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.PAID);

        Mockito.when(orderRepository.findByIdWithFlashSaleItem(orderId)).thenReturn(Optional.of(order));
        Mockito.when(flashSaleItemRepository.decrementSoldCount(flashSaleItemId, 5)).thenReturn(1);

        orderService.handleRefund(orderId);

        assertEquals(OrderStatus.REFUNDED, order.getStatus());
        Mockito.verify(flashSaleItemRepository).decrementSoldCount(flashSaleItemId, 5);
        Mockito.verify(notificationService).sendRefundNotification(userId, orderId);
    }

    @Test
    public void shouldProcessFailedPayment() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.FAILED);

        Mockito.when(orderRepository.findByIdWithFlashSaleItem(orderId)).thenReturn(Optional.of(order));
        Mockito.when(flashSaleItemRepository.decrementSoldCount(flashSaleItemId, 5)).thenReturn(1);

        orderService.processFailedPayment(orderId);

        assertEquals(OrderStatus.FAILED, order.getStatus());
        Mockito.verify(flashSaleItemRepository).decrementSoldCount(flashSaleItemId, 5);
        Mockito.verify(notificationService).sendPaymentFailedNotification(userId, orderId);
    }

    @Test
    public void shouldProcessDispatchSuccessfully() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.PAID);

        Mockito.when(orderRepository.findByIdWithProduct(orderId)).thenReturn(Optional.of(order));
        Mockito.when(productRepository.decrementStock(productId, 5)).thenReturn(1);

        orderService.processDispatch(orderId);

        assertEquals(OrderStatus.DISPATCHED, order.getStatus());
        Mockito.verify(productRepository).decrementStock(productId, 5);
        Mockito.verify(notificationService).sendDispatchNotification(userId, orderId);
    }

    @Test
    public void shouldFailDispatchWhenOrderNotPaid() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING);

        Mockito.when(orderRepository.findByIdWithProduct(orderId)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStatusException.class, () -> {
            orderService.processDispatch(orderId);
        });
    }

    @Test
    public void shouldFailRefundWhenOrderNotPaid() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING);

        Mockito.when(orderRepository.findByIdWithFlashSaleItem(orderId)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStatusException.class, () -> {
            orderService.handleRefund(orderId);
        });
    }

    @Test
    public void shouldFailWhenOrderNotFound() {
        final UUID orderId = UUID.randomUUID();
        Mockito.when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> {
            orderService.processOrderPayment(orderId);
        });
    }

    @Test
    public void shouldGetOrderByIdSuccessfully() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.PAID);
        order.setCreatedAt(OffsetDateTime.now());

        Mockito.when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        final OrderDetailDto result = orderService.getOrderById(orderId, userId);

        assertNotNull(result);
        assertEquals(orderId, result.orderId());
        assertEquals(userId, result.userId());
        assertEquals(productId, result.productId());
        assertEquals(product.getName(), result.productName());
        assertEquals(flashSaleItemId, result.flashSaleItemId());
        assertEquals(flashSaleId, result.flashSaleId());
        assertEquals(flashSale.getTitle(), result.flashSaleTitle());
        assertEquals(BigDecimal.valueOf(79.99), result.soldPrice());
        assertEquals(5, result.soldQuantity());
        assertEquals(BigDecimal.valueOf(399.95), result.totalAmount());
        assertEquals(OrderStatus.PAID, result.status());
        assertNotNull(result.createdAt());
    }

    @Test
    public void shouldFailGetOrderByIdWhenOrderNotFound() {
        final UUID orderId = UUID.randomUUID();
        Mockito.when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrderById(orderId, userId);
        });
    }

    @Test
    public void shouldFailGetOrderByIdWhenOrderBelongsToDifferentUser() {
        final UUID orderId = UUID.randomUUID();
        Mockito.when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrderById(orderId, userId);
        });
    }

    @Test
    public void shouldFindOrderByUserAndFlashSaleItemWhenOrderExists() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.PAID);
        order.setCreatedAt(OffsetDateTime.now());

        Mockito.when(orderRepository.findByUserIdAndFlashSaleItemId(userId, flashSaleItemId))
            .thenReturn(Optional.of(order));

        final Optional<OrderDetailDto> result = orderService.findOrderByUserAndFlashSaleItem(userId, flashSaleItemId);

        assertTrue(result.isPresent());
        assertEquals(orderId, result.get().orderId());
        assertEquals(userId, result.get().userId());
        assertEquals(flashSaleItemId, result.get().flashSaleItemId());
        assertEquals(BigDecimal.valueOf(79.99), result.get().soldPrice());
        assertEquals(5, result.get().soldQuantity());
        assertEquals(OrderStatus.PAID, result.get().status());
    }

    @Test
    public void shouldFindOrderByUserAndFlashSaleItemReturnEmptyWhenNoOrder() {
        Mockito.when(orderRepository.findByUserIdAndFlashSaleItemId(userId, flashSaleItemId))
            .thenReturn(Optional.empty());

        final Optional<OrderDetailDto> result = orderService.findOrderByUserAndFlashSaleItem(userId, flashSaleItemId);

        assertFalse(result.isPresent());
    }

    @Test
    public void shouldGetOrdersByUserWithoutFilters() {
        final UUID orderId1 = UUID.randomUUID();
        final UUID orderId2 = UUID.randomUUID();
        final OffsetDateTime now = OffsetDateTime.now();

        final Order order1 = new Order();
        order1.setId(orderId1);
        order1.setUserId(userId);
        order1.setFlashSaleItem(flashSaleItem);
        order1.setProduct(product);
        order1.setSoldPrice(BigDecimal.valueOf(79.99));
        order1.setSoldQuantity(5);
        order1.setStatus(OrderStatus.PAID);
        order1.setCreatedAt(now.minusDays(1));

        final Order order2 = new Order();
        order2.setId(orderId2);
        order2.setUserId(userId);
        order2.setFlashSaleItem(flashSaleItem);
        order2.setProduct(product);
        order2.setSoldPrice(BigDecimal.valueOf(89.99));
        order2.setSoldQuantity(3);
        order2.setStatus(OrderStatus.PENDING);
        order2.setCreatedAt(now);

        Mockito.when(orderRepository.findByUserId(userId)).thenReturn(List.of(order2, order1));

        final List<OrderDetailDto> result = orderService.getOrdersByUser(userId, null, null, null);

        assertNotNull(result);
        assertEquals(2, result.size());
        // Should be ordered by createdAt DESC (most recent first)
        assertEquals(orderId2, result.get(0).orderId());
        assertEquals(orderId1, result.get(1).orderId());
    }

    @Test
    public void shouldGetOrdersByUserWithStatusFilter() {
        final UUID orderId1 = UUID.randomUUID();
        final OffsetDateTime now = OffsetDateTime.now();

        final Order order1 = new Order();
        order1.setId(orderId1);
        order1.setUserId(userId);
        order1.setFlashSaleItem(flashSaleItem);
        order1.setProduct(product);
        order1.setSoldPrice(BigDecimal.valueOf(79.99));
        order1.setSoldQuantity(5);
        order1.setStatus(OrderStatus.PAID);
        order1.setCreatedAt(now);

        Mockito.when(orderRepository.findByUserIdAndStatus(userId, OrderStatus.PAID)).thenReturn(List.of(order1));

        final List<OrderDetailDto> result = orderService.getOrdersByUser(userId, OrderStatus.PAID, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(orderId1, result.get(0).orderId());
        assertEquals(OrderStatus.PAID, result.get(0).status());
    }

    @Test
    public void shouldGetOrdersByUserWithDateRangeFilter() {
        final UUID orderId1 = UUID.randomUUID();
        final OffsetDateTime startDate = OffsetDateTime.now().minusDays(7);
        final OffsetDateTime endDate = OffsetDateTime.now();
        final OffsetDateTime orderDate = OffsetDateTime.now().minusDays(3);

        final Order order1 = new Order();
        order1.setId(orderId1);
        order1.setUserId(userId);
        order1.setFlashSaleItem(flashSaleItem);
        order1.setProduct(product);
        order1.setSoldPrice(BigDecimal.valueOf(79.99));
        order1.setSoldQuantity(5);
        order1.setStatus(OrderStatus.PAID);
        order1.setCreatedAt(orderDate);

        Mockito.when(orderRepository.findByUserIdAndCreatedAtBetween(userId, startDate, endDate))
            .thenReturn(List.of(order1));

        final List<OrderDetailDto> result = orderService.getOrdersByUser(userId, null, startDate, endDate);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(orderId1, result.get(0).orderId());
    }

    @Test
    public void shouldGetOrdersByUserWithAllFilters() {
        final UUID orderId1 = UUID.randomUUID();
        final OffsetDateTime startDate = OffsetDateTime.now().minusDays(7);
        final OffsetDateTime endDate = OffsetDateTime.now();
        final OffsetDateTime orderDate = OffsetDateTime.now().minusDays(3);

        final Order order1 = new Order();
        order1.setId(orderId1);
        order1.setUserId(userId);
        order1.setFlashSaleItem(flashSaleItem);
        order1.setProduct(product);
        order1.setSoldPrice(BigDecimal.valueOf(79.99));
        order1.setSoldQuantity(5);
        order1.setStatus(OrderStatus.PAID);
        order1.setCreatedAt(orderDate);

        Mockito.when(orderRepository.findByUserIdAndStatusAndCreatedAtBetween(userId, OrderStatus.PAID, startDate, endDate))
            .thenReturn(List.of(order1));

        final List<OrderDetailDto> result = orderService.getOrdersByUser(userId, OrderStatus.PAID, startDate, endDate);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(orderId1, result.get(0).orderId());
        assertEquals(OrderStatus.PAID, result.get(0).status());
    }

    @Test
    public void shouldFailGetOrdersByUserWithInvalidDateRange() {
        final OffsetDateTime startDate = OffsetDateTime.now();
        final OffsetDateTime endDate = OffsetDateTime.now().minusDays(1); // endDate before startDate

        assertThrows(IllegalArgumentException.class, () -> {
            orderService.getOrdersByUser(userId, null, startDate, endDate);
        });
    }

    @Test
    public void shouldReturnEmptyListWhenNoOrdersFound() {
        Mockito.when(orderRepository.findByUserId(userId)).thenReturn(List.of());

        final List<OrderDetailDto> result = orderService.getOrdersByUser(userId, null, null, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // Admin methods tests

    @Test
    public void shouldGetAllOrdersWithoutFilters() {
        final UUID orderId1 = UUID.randomUUID();
        final UUID orderId2 = UUID.randomUUID();
        final OffsetDateTime now = OffsetDateTime.now();

        final Order order1 = new Order();
        order1.setId(orderId1);
        order1.setUserId(userId);
        order1.setFlashSaleItem(flashSaleItem);
        order1.setProduct(product);
        order1.setSoldPrice(BigDecimal.valueOf(79.99));
        order1.setSoldQuantity(5);
        order1.setStatus(OrderStatus.PAID);
        order1.setCreatedAt(now.minusDays(1));

        final Order order2 = new Order();
        order2.setId(orderId2);
        order2.setUserId(userId);
        order2.setFlashSaleItem(flashSaleItem);
        order2.setProduct(product);
        order2.setSoldPrice(BigDecimal.valueOf(89.99));
        order2.setSoldQuantity(3);
        order2.setStatus(OrderStatus.PENDING);
        order2.setCreatedAt(now);

        Mockito.when(orderRepository.findAllWithFilters(null, null, null, null))
            .thenReturn(List.of(order2, order1));

        final List<OrderDetailDto> result = orderService.getAllOrders(null, null, null, null);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(orderId2, result.get(0).orderId());
        assertEquals(orderId1, result.get(1).orderId());
    }

    @Test
    public void shouldGetAllOrdersWithStatusFilter() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.PAID);
        order.setCreatedAt(OffsetDateTime.now());

        Mockito.when(orderRepository.findAllWithFilters(OrderStatus.PAID, null, null, null))
            .thenReturn(List.of(order));

        final List<OrderDetailDto> result = orderService.getAllOrders(OrderStatus.PAID, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(OrderStatus.PAID, result.get(0).status());
    }

    @Test
    public void shouldGetAllOrdersWithAllFilters() {
        final UUID filterUserId = UUID.randomUUID();
        final UUID orderId = UUID.randomUUID();
        final OffsetDateTime startDate = OffsetDateTime.now().minusDays(7);
        final OffsetDateTime endDate = OffsetDateTime.now();

        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(filterUserId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.PAID);
        order.setCreatedAt(OffsetDateTime.now().minusDays(3));

        Mockito.when(orderRepository.findAllWithFilters(OrderStatus.PAID, startDate, endDate, filterUserId))
            .thenReturn(List.of(order));

        final List<OrderDetailDto> result = orderService.getAllOrders(OrderStatus.PAID, startDate, endDate, filterUserId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(OrderStatus.PAID, result.get(0).status());
    }

    @Test
    public void shouldGetOrderByIdForAdminSuccessfully() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.PAID);
        order.setCreatedAt(OffsetDateTime.now());

        Mockito.when(orderRepository.findByIdForAdmin(orderId)).thenReturn(Optional.of(order));

        final OrderDetailDto result = orderService.getOrderByIdForAdmin(orderId);

        assertNotNull(result);
        assertEquals(orderId, result.orderId());
        assertEquals(OrderStatus.PAID, result.status());
    }

    @Test
    public void shouldFailGetOrderByIdForAdminWhenNotFound() {
        final UUID orderId = UUID.randomUUID();
        Mockito.when(orderRepository.findByIdForAdmin(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrderByIdForAdmin(orderId);
        });
    }

    // updateOrderStatus tests

    @Test
    public void shouldUpdateStatusFromPendingToPaid() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.PENDING);

        Mockito.when(orderRepository.findByIdForAdmin(orderId)).thenReturn(Optional.of(order));
        Mockito.when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.updateOrderStatus(orderId, OrderStatus.PAID);

        assertEquals(OrderStatus.PAID, order.getStatus());
        Mockito.verify(orderRepository).save(order);
        Mockito.verify(orderStatusHistoryRepository).save(argThat((OrderStatusHistory h) ->
            orderId.equals(h.getOrderId()) && h.getFromStatus() == OrderStatus.PENDING && h.getToStatus() == OrderStatus.PAID));
        Mockito.verify(auditLogService).recordAdminAction(eq(AuditLogService.ACTION_UPDATE_ORDER_STATUS), eq(AuditLogService.ENTITY_ORDER), eq(orderId), any());
    }

    @Test
    public void shouldUpdateStatusFromPaidToDispatched() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.PAID);

        Mockito.when(orderRepository.findByIdForAdmin(orderId)).thenReturn(Optional.of(order));
        Mockito.when(productRepository.decrementStock(productId, 5)).thenReturn(1);
        Mockito.when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.updateOrderStatus(orderId, OrderStatus.DISPATCHED);

        assertEquals(OrderStatus.DISPATCHED, order.getStatus());
        Mockito.verify(productRepository).decrementStock(productId, 5);
        Mockito.verify(orderRepository).save(order);
    }

    @Test
    public void shouldUpdateStatusFromPaidToRefunded() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.PAID);

        Mockito.when(orderRepository.findByIdForAdmin(orderId)).thenReturn(Optional.of(order));
        Mockito.when(flashSaleItemRepository.decrementSoldCount(flashSaleItemId, 5)).thenReturn(1);
        Mockito.when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.updateOrderStatus(orderId, OrderStatus.REFUNDED);

        assertEquals(OrderStatus.REFUNDED, order.getStatus());
        Mockito.verify(flashSaleItemRepository).decrementSoldCount(flashSaleItemId, 5);
        Mockito.verify(orderRepository).save(order);
    }

    @Test
    public void shouldUpdateStatusFromDispatchedToPaid() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.DISPATCHED);

        Mockito.when(orderRepository.findByIdForAdmin(orderId)).thenReturn(Optional.of(order));
        Mockito.when(productRepository.incrementStock(productId, 5)).thenReturn(1);
        Mockito.when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.updateOrderStatus(orderId, OrderStatus.PAID);

        assertEquals(OrderStatus.PAID, order.getStatus());
        Mockito.verify(productRepository).incrementStock(productId, 5);
        Mockito.verify(orderRepository).save(order);
    }

    @Test
    public void shouldUpdateStatusFromRefundedToPaid() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.REFUNDED);

        Mockito.when(orderRepository.findByIdForAdmin(orderId)).thenReturn(Optional.of(order));
        Mockito.when(flashSaleItemRepository.incrementSoldCountForAdmin(flashSaleItemId, 5)).thenReturn(1);
        Mockito.when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.updateOrderStatus(orderId, OrderStatus.PAID);

        assertEquals(OrderStatus.PAID, order.getStatus());
        Mockito.verify(flashSaleItemRepository).incrementSoldCountForAdmin(flashSaleItemId, 5);
        Mockito.verify(orderRepository).save(order);
    }

    @Test
    public void shouldUpdateStatusFromPendingToFailed() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.PENDING);

        Mockito.when(orderRepository.findByIdForAdmin(orderId)).thenReturn(Optional.of(order));
        Mockito.when(flashSaleItemRepository.decrementSoldCount(flashSaleItemId, 5)).thenReturn(1);
        Mockito.when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.updateOrderStatus(orderId, OrderStatus.FAILED);

        assertEquals(OrderStatus.FAILED, order.getStatus());
        Mockito.verify(flashSaleItemRepository).decrementSoldCount(flashSaleItemId, 5);
        Mockito.verify(orderRepository).save(order);
    }

    @Test
    public void shouldUpdateStatusFromFailedToPending() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.FAILED);

        Mockito.when(orderRepository.findByIdForAdmin(orderId)).thenReturn(Optional.of(order));
        Mockito.when(flashSaleItemRepository.incrementSoldCountForAdmin(flashSaleItemId, 5)).thenReturn(1);
        Mockito.when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.updateOrderStatus(orderId, OrderStatus.PENDING);

        assertEquals(OrderStatus.PENDING, order.getStatus());
        Mockito.verify(flashSaleItemRepository).incrementSoldCountForAdmin(flashSaleItemId, 5);
        Mockito.verify(orderRepository).save(order);
    }

    @Test
    public void shouldNotUpdateWhenStatusUnchanged() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PAID);

        Mockito.when(orderRepository.findByIdForAdmin(orderId)).thenReturn(Optional.of(order));

        orderService.updateOrderStatus(orderId, OrderStatus.PAID);

        assertEquals(OrderStatus.PAID, order.getStatus());
        Mockito.verify(orderRepository, Mockito.never()).save(any(Order.class));
    }

    @Test
    public void shouldFailUpdateStatusWhenOrderNotFound() {
        final UUID orderId = UUID.randomUUID();
        Mockito.when(orderRepository.findByIdForAdmin(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> {
            orderService.updateOrderStatus(orderId, OrderStatus.DISPATCHED);
        });
    }

    @Test
    public void shouldFailUpdateStatusForInvalidTransition() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.DISPATCHED);

        Mockito.when(orderRepository.findByIdForAdmin(orderId)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStatusException.class, () -> {
            orderService.updateOrderStatus(orderId, OrderStatus.PENDING);
        });
    }

    @Test
    public void shouldFailUpdateStatusWhenStockOperationFails() {
        final UUID orderId = UUID.randomUUID();
        final Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(product);
        order.setSoldPrice(BigDecimal.valueOf(79.99));
        order.setSoldQuantity(5);
        order.setStatus(OrderStatus.PAID);

        Mockito.when(orderRepository.findByIdForAdmin(orderId)).thenReturn(Optional.of(order));
        Mockito.when(productRepository.decrementStock(productId, 5)).thenReturn(0); // Stock operation fails

        assertThrows(IllegalStateException.class, () -> {
            orderService.updateOrderStatus(orderId, OrderStatus.DISPATCHED);
        });
    }
}
