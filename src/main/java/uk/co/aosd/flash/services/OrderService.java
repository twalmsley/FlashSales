package uk.co.aosd.flash.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.co.aosd.flash.config.RabbitMQConfig;
import uk.co.aosd.flash.domain.FlashSaleItem;
import uk.co.aosd.flash.domain.Order;
import uk.co.aosd.flash.domain.OrderStatus;
import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.CreateOrderDto;
import uk.co.aosd.flash.dto.OrderDetailDto;
import uk.co.aosd.flash.dto.OrderResponseDto;
import uk.co.aosd.flash.exc.InsufficientStockException;
import uk.co.aosd.flash.exc.InvalidOrderStatusException;
import uk.co.aosd.flash.exc.OrderNotFoundException;
import uk.co.aosd.flash.exc.SaleNotActiveException;
import uk.co.aosd.flash.repository.FlashSaleItemRepository;
import uk.co.aosd.flash.repository.OrderRepository;
import uk.co.aosd.flash.repository.ProductRepository;

/**
 * Service for managing orders.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final ProductRepository productRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Create a new order for an active sale.
     * Validates that the sale is still active (checks end time), checks stock availability,
     * creates a PENDING order, increments sold count, and queues the order for processing.
     *
     * @param createOrderDto the order creation DTO
     * @param userId the user ID (extracted from JWT token)
     * @return OrderResponseDto with order status
     * @throws SaleNotActiveException      if the sale has ended
     * @throws InsufficientStockException if there's not enough stock
     */
    @Transactional
    @CacheEvict(value = {"orders:user", "activeSales"}, key = "#userId", allEntries = true)
    public OrderResponseDto createOrder(@Valid final CreateOrderDto createOrderDto, final UUID userId) {
        log.info("Creating order for user {} for flash sale item {}", userId, createOrderDto.flashSaleItemId());

        // Load flash sale item with flash sale
        final FlashSaleItem flashSaleItem = flashSaleItemRepository.findById(createOrderDto.flashSaleItemId())
            .orElseThrow(() -> {
                log.error("Flash sale item not found: {}", createOrderDto.flashSaleItemId());
                return new OrderNotFoundException(createOrderDto.flashSaleItemId());
            });

        // Check if sale is still active by checking end time (not status)
        final OffsetDateTime now = OffsetDateTime.now();
        if (flashSaleItem.getFlashSale().getEndTime().isBefore(now) || flashSaleItem.getFlashSale().getEndTime().isEqual(now)) {
            log.warn("Sale has ended. End time: {}, Current time: {}", flashSaleItem.getFlashSale().getEndTime(), now);
            throw new SaleNotActiveException(
                flashSaleItem.getFlashSale().getId(),
                flashSaleItem.getFlashSale().getEndTime(),
                now);
        }

        // Check stock availability
        final int availableStock = flashSaleItem.getAllocatedStock() - flashSaleItem.getSoldCount();
        if (availableStock < createOrderDto.quantity()) {
            log.warn("Insufficient stock. Available: {}, Requested: {}", availableStock, createOrderDto.quantity());
            throw new InsufficientStockException(
                createOrderDto.flashSaleItemId(),
                createOrderDto.quantity(),
                availableStock);
        }

        // Check sale status is ACTIVE (required for incrementSoldCount)
        if (flashSaleItem.getFlashSale().getStatus() != SaleStatus.ACTIVE) {
            log.warn("Sale is not ACTIVE. Current status: {}", flashSaleItem.getFlashSale().getStatus());
            throw new SaleNotActiveException(
                flashSaleItem.getFlashSale().getId(),
                flashSaleItem.getFlashSale().getEndTime(),
                now);
        }

        // Atomically increment sold count FIRST (before creating order)
        // This ensures we don't create orders that can't be fulfilled
        final int updated = flashSaleItemRepository.incrementSoldCount(flashSaleItem.getId(), createOrderDto.quantity());
        if (updated == 0) {
            log.error("Failed to increment sold count for flash sale item {}. Sale may not be ACTIVE or stock insufficient.", flashSaleItem.getId());
            throw new InsufficientStockException(
                createOrderDto.flashSaleItemId(),
                createOrderDto.quantity(),
                availableStock);
        }

        // Create order AFTER successful increment
        final Order order = new Order();
        order.setUserId(userId);
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(flashSaleItem.getProduct());
        order.setSoldPrice(flashSaleItem.getSalePrice());
        order.setSoldQuantity(createOrderDto.quantity());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(OffsetDateTime.now());

        final Order savedOrder = orderRepository.save(order);
        log.info("Created order: {}", savedOrder.getId());

        // Send order confirmation notification
        notificationService.sendOrderConfirmation(userId, savedOrder.getId());

        // Queue order for processing AFTER transaction commits
        // This ensures the order is visible in the database when the consumer processes it
        final UUID orderIdToQueue = savedOrder.getId();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rabbitTemplate.convertAndSend(
                        RabbitMQConfig.ORDER_EXCHANGE,
                        RabbitMQConfig.ROUTING_KEY_PROCESSING,
                        orderIdToQueue.toString());
                    log.info("Queued order {} for processing (after transaction commit)", orderIdToQueue);
                }
            });
        } else {
            // No active transaction, send immediately (e.g., in tests)
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_PROCESSING,
                orderIdToQueue.toString());
            log.info("Queued order {} for processing", orderIdToQueue);
        }

        return new OrderResponseDto(savedOrder.getId(), OrderStatus.PENDING, "Order created and queued for processing");
    }

    /**
     * Process payment for an order.
     * Attempts to take payment - success means queue for dispatch, failed means queue for failed payment handling.
     *
     * @param orderId the order ID
     */
    @Transactional
    @CacheEvict(value = {"orders", "orders:user"}, allEntries = true)
    public void processOrderPayment(final UUID orderId) {
        log.info("Processing payment for order {}", orderId);

        final Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> {
                log.error("Order not found: {}", orderId);
                return new OrderNotFoundException(orderId);
            });

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Order {} is not in PENDING status. Current status: {}", orderId, order.getStatus());
            throw new InvalidOrderStatusException(orderId, order.getStatus(), OrderStatus.PENDING, "process payment");
        }

        final BigDecimal totalAmount = order.getSoldPrice().multiply(BigDecimal.valueOf(order.getSoldQuantity()));
        final boolean paymentSuccess = paymentService.processPayment(orderId, totalAmount);

        if (paymentSuccess) {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
            log.info("Payment succeeded for order {}. Status updated to PAID", orderId);

            // Queue for dispatch AFTER transaction commits
            final UUID dispatchOrderId = orderId;
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        rabbitTemplate.convertAndSend(
                            RabbitMQConfig.ORDER_EXCHANGE,
                            RabbitMQConfig.ROUTING_KEY_DISPATCH,
                            dispatchOrderId.toString());
                        log.info("Queued order {} for dispatch (after transaction commit)", dispatchOrderId);
                    }
                });
            } else {
                rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY_DISPATCH,
                    dispatchOrderId.toString());
                log.info("Queued order {} for dispatch", dispatchOrderId);
            }
        } else {
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            log.warn("Payment failed for order {}. Status updated to FAILED", orderId);

            // Queue for failed payment handling AFTER transaction commits
            final UUID failedOrderId = orderId;
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        rabbitTemplate.convertAndSend(
                            RabbitMQConfig.ORDER_EXCHANGE,
                            RabbitMQConfig.ROUTING_KEY_PAYMENT_FAILED,
                            failedOrderId.toString());
                        log.info("Queued order {} for failed payment handling (after transaction commit)", failedOrderId);
                    }
                });
            } else {
                rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY_PAYMENT_FAILED,
                    failedOrderId.toString());
                log.info("Queued order {} for failed payment handling", failedOrderId);
            }
        }
    }

    /**
     * Handle refund of a PAID order.
     * Decreases the stock sold count for the sales item, changes the order status to REFUNDED,
     * queues the order for refunding, and notifies the user.
     *
     * @param orderId the order ID
     */
    @Transactional
    @CacheEvict(value = {"orders", "orders:user"}, allEntries = true)
    public void handleRefund(final UUID orderId) {
        log.info("Handling refund for order {}", orderId);

        final Order order = orderRepository.findByIdWithFlashSaleItem(orderId)
            .orElseThrow(() -> {
                log.error("Order not found: {}", orderId);
                return new OrderNotFoundException(orderId);
            });

        if (order.getStatus() != OrderStatus.PAID) {
            log.warn("Order {} is not in PAID status. Current status: {}", orderId, order.getStatus());
            throw new InvalidOrderStatusException(orderId, order.getStatus(), OrderStatus.PAID, "refund");
        }

        // Decrement sold count
        final int updated = flashSaleItemRepository.decrementSoldCount(
            order.getFlashSaleItem().getId(),
            order.getSoldQuantity());

        if (updated == 0) {
            log.error("Failed to decrement sold count for flash sale item {}", order.getFlashSaleItem().getId());
            throw new IllegalStateException("Failed to decrement sold count for refund");
        }

        // Update order status
        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);
        log.info("Order {} status updated to REFUNDED", orderId);

        // Queue for refund notification AFTER transaction commits
        final UUID refundOrderId = orderId;
        final UUID refundUserId = order.getUserId();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rabbitTemplate.convertAndSend(
                        RabbitMQConfig.ORDER_EXCHANGE,
                        RabbitMQConfig.ROUTING_KEY_REFUND,
                        refundOrderId.toString());
                    log.info("Queued order {} for refund notification (after transaction commit)", refundOrderId);
                }
            });
        } else {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_REFUND,
                refundOrderId.toString());
            log.info("Queued order {} for refund notification", refundOrderId);
        }

        // Notify user (can be done synchronously as it's just logging)
        notificationService.sendRefundNotification(refundUserId, refundOrderId);
    }

    /**
     * Process failed payment queue.
     * Decreases the stock sold count for the sales item, changes the order status to FAILED,
     * and notifies the user.
     *
     * @param orderId the order ID
     */
    @Transactional
    @CacheEvict(value = {"orders", "orders:user"}, allEntries = true)
    public void processFailedPayment(final UUID orderId) {
        log.info("Processing failed payment for order {}", orderId);

        final Order order = orderRepository.findByIdWithFlashSaleItem(orderId)
            .orElseThrow(() -> {
                log.error("Order not found: {}", orderId);
                return new OrderNotFoundException(orderId);
            });

        // Decrement sold count
        final int updated = flashSaleItemRepository.decrementSoldCount(
            order.getFlashSaleItem().getId(),
            order.getSoldQuantity());

        if (updated == 0) {
            log.error("Failed to decrement sold count for flash sale item {}", order.getFlashSaleItem().getId());
            throw new IllegalStateException("Failed to decrement sold count for failed payment");
        }

        // Order status should already be FAILED from processOrderPayment, but ensure it is
        if (order.getStatus() != OrderStatus.FAILED) {
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
        }

        log.info("Processed failed payment for order {}", orderId);

        // Notify user
        notificationService.sendPaymentFailedNotification(order.getUserId(), orderId);
    }

    /**
     * Process dispatch order queue.
     * Makes sure the order is in the PAID state (i.e. it hasn't been refunded),
     * updates the order status to DISPATCHED, decreases the total physical stock AND
     * the reserved stock for the product (not the sales item), and notifies the user.
     *
     * @param orderId the order ID
     */
    @Transactional
    @CacheEvict(value = {"orders", "orders:user"}, allEntries = true)
    public void processDispatch(final UUID orderId) {
        log.info("Processing dispatch for order {}", orderId);

        final Order order = orderRepository.findByIdWithProduct(orderId)
            .orElseThrow(() -> {
                log.error("Order not found: {}", orderId);
                return new OrderNotFoundException(orderId);
            });

        // Make sure order is in PAID state (hasn't been refunded)
        if (order.getStatus() != OrderStatus.PAID) {
            log.warn("Order {} is not in PAID status. Current status: {}. Cannot dispatch.", orderId, order.getStatus());
            throw new InvalidOrderStatusException(orderId, order.getStatus(), OrderStatus.PAID, "dispatch");
        }

        // Decrement product stock (both totalPhysicalStock and reservedCount)
        final int updated = productRepository.decrementStock(order.getProduct().getId(), order.getSoldQuantity());

        if (updated == 0) {
            log.error("Failed to decrement stock for product {}", order.getProduct().getId());
            throw new IllegalStateException("Failed to decrement product stock for dispatch");
        }

        // Update order status to DISPATCHED
        order.setStatus(OrderStatus.DISPATCHED);
        orderRepository.save(order);
        log.info("Order {} status updated to DISPATCHED", orderId);

        // Notify user
        notificationService.sendDispatchNotification(order.getUserId(), orderId);
    }

    /**
     * Get order details by ID for a specific user.
     * Validates that the order belongs to the user.
     *
     * @param orderId the order ID
     * @param userId the user ID
     * @return OrderDetailDto with complete order information
     * @throws OrderNotFoundException if order doesn't exist or doesn't belong to user
     */
    @Cacheable(value = "orders", key = "#orderId + ':' + #userId")
    public OrderDetailDto getOrderById(final UUID orderId, final UUID userId) {
        log.info("Fetching order {} for user {}", orderId, userId);

        final Order order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> {
                log.warn("Order {} not found for user {}", orderId, userId);
                return new OrderNotFoundException(orderId);
            });

        return mapToOrderDetailDto(order);
    }

    /**
     * Get orders for a user with optional filters (status, date range).
     * Results are ordered by createdAt descending (most recent first).
     *
     * @param userId the user ID
     * @param status optional status filter
     * @param startDate optional start date filter (inclusive)
     * @param endDate optional end date filter (inclusive)
     * @return list of OrderDetailDto matching the criteria
     * @throws IllegalArgumentException if date range is invalid (startDate > endDate)
     */
    @Cacheable(value = "orders:user", key = "#userId + ':' + (#status != null ? #status.toString() : 'null') + ':' + (#startDate != null ? #startDate.toString() : 'null') + ':' + (#endDate != null ? #endDate.toString() : 'null')")
    public List<OrderDetailDto> getOrdersByUser(
        final UUID userId,
        final OrderStatus status,
        final OffsetDateTime startDate,
        final OffsetDateTime endDate) {

        log.info("Fetching orders for user {} with filters: status={}, startDate={}, endDate={}",
            userId, status, startDate, endDate);

        // Validate date range if both dates are provided
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        final List<Order> orders;
        if (status != null && startDate != null && endDate != null) {
            // All filters
            orders = orderRepository.findByUserIdAndStatusAndCreatedAtBetween(userId, status, startDate, endDate);
        } else if (status != null) {
            // Status only
            orders = orderRepository.findByUserIdAndStatus(userId, status);
        } else if (startDate != null && endDate != null) {
            // Date range only
            orders = orderRepository.findByUserIdAndCreatedAtBetween(userId, startDate, endDate);
        } else {
            // No filters
            orders = orderRepository.findByUserId(userId);
        }

        log.info("Found {} orders for user {}", orders.size(), userId);
        return orders.stream()
            .map(this::mapToOrderDetailDto)
            .toList();
    }

    /**
     * Get all orders with optional filters for admin use.
     * Results are ordered by createdAt descending (most recent first).
     *
     * @param status optional status filter
     * @param startDate optional start date filter (inclusive)
     * @param endDate optional end date filter (inclusive)
     * @param userId optional user ID filter
     * @return list of OrderDetailDto matching the criteria
     * @throws IllegalArgumentException if date range is invalid (startDate > endDate)
     */
    @Cacheable(value = "orders:all", key = "(#status != null ? #status.toString() : 'null') + ':' + (#startDate != null ? #startDate.toString() : 'null') + ':' + (#endDate != null ? #endDate.toString() : 'null') + ':' + (#userId != null ? #userId.toString() : 'null')")
    public List<OrderDetailDto> getAllOrders(
        final OrderStatus status,
        final OffsetDateTime startDate,
        final OffsetDateTime endDate,
        final UUID userId) {

        log.info("Fetching all orders with filters: status={}, startDate={}, endDate={}, userId={}",
            status, startDate, endDate, userId);

        // Validate date range if both dates are provided
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        final List<Order> orders = orderRepository.findAllWithFilters(status, startDate, endDate, userId);

        log.info("Found {} orders", orders.size());
        return orders.stream()
            .map(this::mapToOrderDetailDto)
            .toList();
    }

    /**
     * Get order details by ID for admin use.
     * Does not validate user ownership.
     *
     * @param orderId the order ID
     * @return OrderDetailDto with complete order information
     * @throws OrderNotFoundException if order doesn't exist
     */
    @Cacheable(value = "orders", key = "#orderId + ':admin'")
    public OrderDetailDto getOrderByIdForAdmin(final UUID orderId) {
        log.info("Fetching order {} for admin", orderId);

        final Order order = orderRepository.findByIdForAdmin(orderId)
            .orElseThrow(() -> {
                log.warn("Order {} not found", orderId);
                return new OrderNotFoundException(orderId);
            });

        return mapToOrderDetailDto(order);
    }

    /**
     * Update order status with proper stock adjustments.
     * Handles all valid status transitions and adjusts stock accordingly.
     *
     * @param orderId the order ID
     * @param newStatus the new status to set
     * @throws OrderNotFoundException if order doesn't exist
     * @throws InvalidOrderStatusException if the transition is invalid
     * @throws IllegalStateException if stock operations fail
     */
    @Transactional
    @CacheEvict(value = {"orders", "orders:user"}, allEntries = true)
    public void updateOrderStatus(final UUID orderId, final OrderStatus newStatus) {
        log.info("Updating order {} status", orderId);

        // Load order with all related entities
        final Order order = orderRepository.findByIdForAdmin(orderId)
            .orElseThrow(() -> {
                log.error("Order not found: {}", orderId);
                return new OrderNotFoundException(orderId);
            });

        final OrderStatus currentStatus = order.getStatus();

        // If status is unchanged, no-op
        if (currentStatus == newStatus) {
            log.info("Order {} already in status {}", orderId, newStatus);
            return;
        }

        log.info("Transitioning order {} from {} to {}", orderId, currentStatus, newStatus);

        // Handle stock adjustments based on transition
        handleStatusTransition(order, currentStatus, newStatus);

        // Update order status
        order.setStatus(newStatus);
        orderRepository.save(order);
        log.info("Order {} status updated to {}", orderId, newStatus);
    }

    /**
     * Handle stock adjustments for status transitions.
     *
     * @param order the order
     * @param fromStatus the current status
     * @param toStatus the new status
     */
    private void handleStatusTransition(final Order order, final OrderStatus fromStatus, final OrderStatus toStatus) {
        final UUID orderId = order.getId();
        final int quantity = order.getSoldQuantity();

        // PENDING → PAID: No stock change (stock already reserved via soldCount)
        if (fromStatus == OrderStatus.PENDING && toStatus == OrderStatus.PAID) {
            log.debug("PENDING → PAID: No stock adjustment needed");
            return;
        }

        // PAID → DISPATCHED: Decrement product stock (both totalPhysicalStock and reservedCount)
        if (fromStatus == OrderStatus.PAID && toStatus == OrderStatus.DISPATCHED) {
            log.debug("PAID → DISPATCHED: Decrementing product stock");
            final int updated = productRepository.decrementStock(order.getProduct().getId(), quantity);
            if (updated == 0) {
                log.error("Failed to decrement stock for product {}", order.getProduct().getId());
                throw new IllegalStateException("Failed to decrement product stock for dispatch");
            }
            return;
        }

        // PAID → REFUNDED: Decrement flash sale item soldCount
        if (fromStatus == OrderStatus.PAID && toStatus == OrderStatus.REFUNDED) {
            log.debug("PAID → REFUNDED: Decrementing flash sale item soldCount");
            final int updated = flashSaleItemRepository.decrementSoldCount(
                order.getFlashSaleItem().getId(), quantity);
            if (updated == 0) {
                log.error("Failed to decrement sold count for flash sale item {}", order.getFlashSaleItem().getId());
                throw new IllegalStateException("Failed to decrement sold count for refund");
            }
            return;
        }

        // DISPATCHED → PAID: Increment product stock (reverse dispatch)
        if (fromStatus == OrderStatus.DISPATCHED && toStatus == OrderStatus.PAID) {
            log.debug("DISPATCHED → PAID: Incrementing product stock (reverse dispatch)");
            final int updated = productRepository.incrementStock(order.getProduct().getId(), quantity);
            if (updated == 0) {
                log.error("Failed to increment stock for product {}", order.getProduct().getId());
                throw new IllegalStateException("Failed to increment product stock for reverse dispatch");
            }
            return;
        }

        // REFUNDED → PAID: Increment flash sale item soldCount (reverse refund)
        if (fromStatus == OrderStatus.REFUNDED && toStatus == OrderStatus.PAID) {
            log.debug("REFUNDED → PAID: Incrementing flash sale item soldCount (reverse refund)");
            final int updated = flashSaleItemRepository.incrementSoldCountForAdmin(
                order.getFlashSaleItem().getId(), quantity);
            if (updated == 0) {
                log.error("Failed to increment sold count for flash sale item {}", order.getFlashSaleItem().getId());
                throw new IllegalStateException("Failed to increment sold count for reverse refund");
            }
            return;
        }

        // PENDING → FAILED: Decrement flash sale item soldCount (release reserved stock)
        if (fromStatus == OrderStatus.PENDING && toStatus == OrderStatus.FAILED) {
            log.debug("PENDING → FAILED: Decrementing flash sale item soldCount");
            final int updated = flashSaleItemRepository.decrementSoldCount(
                order.getFlashSaleItem().getId(), quantity);
            if (updated == 0) {
                log.error("Failed to decrement sold count for flash sale item {}", order.getFlashSaleItem().getId());
                throw new IllegalStateException("Failed to decrement sold count for failed order");
            }
            return;
        }

        // FAILED → PENDING: Increment flash sale item soldCount (re-reserve stock)
        if (fromStatus == OrderStatus.FAILED && toStatus == OrderStatus.PENDING) {
            log.debug("FAILED → PENDING: Incrementing flash sale item soldCount (re-reserve stock)");
            final int updated = flashSaleItemRepository.incrementSoldCountForAdmin(
                order.getFlashSaleItem().getId(), quantity);
            if (updated == 0) {
                log.error("Failed to increment sold count for flash sale item {}", order.getFlashSaleItem().getId());
                throw new IllegalStateException("Failed to increment sold count for re-reserve");
            }
            return;
        }

        // Invalid transition
        log.warn("Invalid status transition: {} → {} for order {}", fromStatus, toStatus, orderId);
        throw new InvalidOrderStatusException(orderId, fromStatus, toStatus, "status update");
    }

    /**
     * Maps an Order entity to OrderDetailDto.
     *
     * @param order the order entity
     * @return OrderDetailDto
     */
    private OrderDetailDto mapToOrderDetailDto(final Order order) {
        final BigDecimal totalAmount = order.getSoldPrice().multiply(BigDecimal.valueOf(order.getSoldQuantity()));

        return new OrderDetailDto(
            order.getId(),
            order.getUserId(),
            order.getProduct().getId(),
            order.getProduct().getName(),
            order.getFlashSaleItem().getId(),
            order.getFlashSaleItem().getFlashSale().getId(),
            order.getFlashSaleItem().getFlashSale().getTitle(),
            order.getSoldPrice(),
            order.getSoldQuantity(),
            totalAmount,
            order.getStatus(),
            order.getCreatedAt());
    }
}
