package uk.co.aosd.flash.services;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.co.aosd.flash.config.RabbitMQConfig;

/**
 * Test OrderMessageSender.
 */
public class OrderMessageSenderTest {

    private RabbitTemplate rabbitTemplate;
    private OrderMessageSender sender;

    @BeforeEach
    public void beforeEach() {
        rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        sender = new OrderMessageSender(rabbitTemplate);
    }

    @Test
    public void sendForProcessing_shouldSendToProcessingRoutingKey() {
        final UUID orderId = UUID.randomUUID();
        sender.sendForProcessing(orderId);
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.ORDER_EXCHANGE),
            eq(RabbitMQConfig.ROUTING_KEY_PROCESSING),
            eq(orderId.toString()));
    }

    @Test
    public void sendForDispatch_shouldSendToDispatchRoutingKey() {
        final UUID orderId = UUID.randomUUID();
        sender.sendForDispatch(orderId);
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.ORDER_EXCHANGE),
            eq(RabbitMQConfig.ROUTING_KEY_DISPATCH),
            eq(orderId.toString()));
    }

    @Test
    public void sendForPaymentFailed_shouldSendToPaymentFailedRoutingKey() {
        final UUID orderId = UUID.randomUUID();
        sender.sendForPaymentFailed(orderId);
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.ORDER_EXCHANGE),
            eq(RabbitMQConfig.ROUTING_KEY_PAYMENT_FAILED),
            eq(orderId.toString()));
    }

    @Test
    public void sendForRefund_shouldSendToRefundRoutingKey() {
        final UUID orderId = UUID.randomUUID();
        sender.sendForRefund(orderId);
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.ORDER_EXCHANGE),
            eq(RabbitMQConfig.ROUTING_KEY_REFUND),
            eq(orderId.toString()));
    }
}
