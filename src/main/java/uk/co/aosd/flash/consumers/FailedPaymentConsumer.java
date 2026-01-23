package uk.co.aosd.flash.consumers;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import uk.co.aosd.flash.config.RabbitMQConfig;
import uk.co.aosd.flash.services.OrderService;

/**
 * Consumer for handling failed payments.
 */
@Component
@RequiredArgsConstructor
public class FailedPaymentConsumer {

    private static final Logger log = LoggerFactory.getLogger(FailedPaymentConsumer.class);

    private final OrderService orderService;

    /**
     * Listen to failed payment queue and process failed payment.
     *
     * @param orderIdStr the order ID as string
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_PAYMENT_FAILED_QUEUE)
    public void processFailedPayment(final String orderIdStr) {
        try {
            final UUID orderId = UUID.fromString(orderIdStr);
            log.info("Received failed payment message for order {}", orderId);
            orderService.processFailedPayment(orderId);
        } catch (final Exception e) {
            log.error("Error processing failed payment for order ID: {}", orderIdStr, e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }
}
