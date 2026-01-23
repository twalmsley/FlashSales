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
 * Consumer for dispatching orders.
 */
@Component
@RequiredArgsConstructor
public class DispatchConsumer {

    private static final Logger log = LoggerFactory.getLogger(DispatchConsumer.class);

    private final OrderService orderService;

    /**
     * Listen to dispatch queue and process dispatch.
     *
     * @param orderIdStr the order ID as string
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_DISPATCH_QUEUE)
    public void processDispatch(final String orderIdStr) {
        try {
            final UUID orderId = UUID.fromString(orderIdStr);
            log.info("Received dispatch message for order {}", orderId);
            orderService.processDispatch(orderId);
        } catch (final Exception e) {
            log.error("Error processing dispatch for order ID: {}", orderIdStr, e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }
}
