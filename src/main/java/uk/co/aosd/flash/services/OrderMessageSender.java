package uk.co.aosd.flash.services;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import uk.co.aosd.flash.config.RabbitMQConfig;

/**
 * Sends order-related messages to RabbitMQ.
 * Callers invoke this after the transactional service method returns (and thus after commit).
 */
@Component
@RequiredArgsConstructor
public class OrderMessageSender {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageSender.class);

    private final RabbitTemplate rabbitTemplate;

    /**
     * Send order for processing (payment).
     */
    public void sendForProcessing(final UUID orderId) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_EXCHANGE,
            RabbitMQConfig.ROUTING_KEY_PROCESSING,
            orderId.toString());
        log.info("Queued order {} for processing", orderId);
    }

    /**
     * Send order for dispatch.
     */
    public void sendForDispatch(final UUID orderId) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_EXCHANGE,
            RabbitMQConfig.ROUTING_KEY_DISPATCH,
            orderId.toString());
        log.info("Queued order {} for dispatch", orderId);
    }

    /**
     * Send order for failed payment handling.
     */
    public void sendForPaymentFailed(final UUID orderId) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_EXCHANGE,
            RabbitMQConfig.ROUTING_KEY_PAYMENT_FAILED,
            orderId.toString());
        log.info("Queued order {} for failed payment handling", orderId);
    }

    /**
     * Send order for refund notification.
     */
    public void sendForRefund(final UUID orderId) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_EXCHANGE,
            RabbitMQConfig.ROUTING_KEY_REFUND,
            orderId.toString());
        log.info("Queued order {} for refund notification", orderId);
    }
}
