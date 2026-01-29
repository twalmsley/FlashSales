package uk.co.aosd.flash.consumers;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import uk.co.aosd.flash.config.RabbitMQConfig;
import uk.co.aosd.flash.services.OrderMessageSender;
import uk.co.aosd.flash.services.OrderService;

/**
 * Consumer for processing orders (payment processing).
 */
@Component
@RequiredArgsConstructor
public class OrderProcessingConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessingConsumer.class);

    private final OrderService orderService;
    private final OrderMessageSender orderMessageSender;

    /**
     * Listen to order processing queue and process payment.
     *
     * @param orderIdStr
     *            the order ID as string
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_PROCESSING_QUEUE)
    public void processOrder(final String orderIdStr) {
        try {
            final UUID orderId = UUID.fromString(orderIdStr);
            log.info("Received order processing message for order {}", orderId);
            final var result = orderService.processOrderPayment(orderId);
            if (result.success()) {
                orderMessageSender.sendForDispatch(result.orderId());
            } else {
                orderMessageSender.sendForPaymentFailed(result.orderId());
            }
        } catch (final Exception e) {
            log.error("Error processing order payment for order ID: {}", orderIdStr, e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }
}
