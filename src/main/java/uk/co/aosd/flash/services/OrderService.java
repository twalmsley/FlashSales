package uk.co.aosd.flash.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import uk.co.aosd.flash.config.RabbitMQConfig;
import uk.co.aosd.flash.domain.FlashSaleItem;
import uk.co.aosd.flash.domain.Order;
import uk.co.aosd.flash.domain.OrderStatus;
import uk.co.aosd.flash.dto.CreateOrderDto;
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
     * @return OrderResponseDto with order status
     * @throws SaleNotActiveException      if the sale has ended
     * @throws InsufficientStockException if there's not enough stock
     */
    @Transactional
    public OrderResponseDto createOrder(@Valid final CreateOrderDto createOrderDto) {
        log.info("Creating order for user {} for flash sale item {}", createOrderDto.userId(), createOrderDto.flashSaleItemId());

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

        // Create order
        final Order order = new Order();
        order.setUserId(createOrderDto.userId());
        order.setFlashSaleItem(flashSaleItem);
        order.setProduct(flashSaleItem.getProduct());
        order.setSoldPrice(flashSaleItem.getSalePrice());
        order.setSoldQuantity(createOrderDto.quantity());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(OffsetDateTime.now());

        final Order savedOrder = orderRepository.save(order);
        log.info("Created order: {}", savedOrder.getId());

        // Atomically increment sold count
        final int updated = flashSaleItemRepository.incrementSoldCount(flashSaleItem.getId(), createOrderDto.quantity());
        if (updated == 0) {
            log.error("Failed to increment sold count for flash sale item {}", flashSaleItem.getId());
            throw new InsufficientStockException(
                createOrderDto.flashSaleItemId(),
                createOrderDto.quantity(),
                availableStock);
        }

        // Queue order for processing
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_EXCHANGE,
            RabbitMQConfig.ROUTING_KEY_PROCESSING,
            savedOrder.getId().toString());

        log.info("Queued order {} for processing", savedOrder.getId());

        return new OrderResponseDto(savedOrder.getId(), OrderStatus.PENDING, "Order created and queued for processing");
    }

    /**
     * Process payment for an order.
     * Attempts to take payment - success means queue for dispatch, failed means queue for failed payment handling.
     *
     * @param orderId the order ID
     */
    @Transactional
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

            // Queue for dispatch
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_DISPATCH,
                orderId.toString());

            log.info("Queued order {} for dispatch", orderId);
        } else {
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            log.warn("Payment failed for order {}. Status updated to FAILED", orderId);

            // Queue for failed payment handling
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_PAYMENT_FAILED,
                orderId.toString());

            log.info("Queued order {} for failed payment handling", orderId);
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

        // Queue for refund notification
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_EXCHANGE,
            RabbitMQConfig.ROUTING_KEY_REFUND,
            orderId.toString());

        log.info("Queued order {} for refund notification", orderId);

        // Notify user
        notificationService.sendRefundNotification(order.getUserId(), orderId);
    }

    /**
     * Process failed payment queue.
     * Decreases the stock sold count for the sales item, changes the order status to FAILED,
     * and notifies the user.
     *
     * @param orderId the order ID
     */
    @Transactional
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
}
