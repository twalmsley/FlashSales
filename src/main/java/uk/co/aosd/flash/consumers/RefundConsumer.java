package uk.co.aosd.flash.consumers;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import uk.co.aosd.flash.config.RabbitMQConfig;
import uk.co.aosd.flash.repository.OrderRepository;
import uk.co.aosd.flash.services.NotificationService;

/**
 * Consumer for processing refund notifications.
 */
@Component
@RequiredArgsConstructor
public class RefundConsumer {

    private static final Logger log = LoggerFactory.getLogger(RefundConsumer.class);

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    /**
     * Listen to refund queue and send refund notification.
     *
     * @param orderIdStr the order ID as string
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_REFUND_QUEUE)
    public void processRefundNotification(final String orderIdStr) {
        try {
            final UUID orderId = UUID.fromString(orderIdStr);
            log.info("Received refund notification message for order {}", orderId);

            final var order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found for refund notification: {}", orderId);
                    return new RuntimeException("Order not found: " + orderId);
                });

            notificationService.sendRefundNotification(order.getUserId(), orderId);
        } catch (final Exception e) {
            log.error("Error processing refund notification for order ID: {}", orderIdStr, e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }
}
