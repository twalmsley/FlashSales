package uk.co.aosd.flash.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for order processing queues.
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    // Exchange names
    public static final String ORDER_EXCHANGE = "order.exchange";

    // Queue names
    public static final String ORDER_PROCESSING_QUEUE = "order.processing";
    public static final String ORDER_PAYMENT_FAILED_QUEUE = "order.payment.failed";
    public static final String ORDER_DISPATCH_QUEUE = "order.dispatch";
    public static final String ORDER_REFUND_QUEUE = "order.refund";

    // Routing keys
    public static final String ROUTING_KEY_PROCESSING = "order.processing";
    public static final String ROUTING_KEY_PAYMENT_FAILED = "order.payment.failed";
    public static final String ROUTING_KEY_DISPATCH = "order.dispatch";
    public static final String ROUTING_KEY_REFUND = "order.refund";

    /**
     * Direct exchange for order-related messages.
     */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    /**
     * Queue for processing new orders (payment processing).
     */
    @Bean
    public Queue orderProcessingQueue() {
        return QueueBuilder.durable(ORDER_PROCESSING_QUEUE).build();
    }

    /**
     * Queue for handling failed payments.
     */
    @Bean
    public Queue orderPaymentFailedQueue() {
        return QueueBuilder.durable(ORDER_PAYMENT_FAILED_QUEUE).build();
    }

    /**
     * Queue for dispatching orders.
     */
    @Bean
    public Queue orderDispatchQueue() {
        return QueueBuilder.durable(ORDER_DISPATCH_QUEUE).build();
    }

    /**
     * Queue for refund processing.
     */
    @Bean
    public Queue orderRefundQueue() {
        return QueueBuilder.durable(ORDER_REFUND_QUEUE).build();
    }

    /**
     * Binding for order processing queue.
     */
    @Bean
    public Binding orderProcessingBinding() {
        return BindingBuilder.bind(orderProcessingQueue())
            .to(orderExchange())
            .with(ROUTING_KEY_PROCESSING);
    }

    /**
     * Binding for payment failed queue.
     */
    @Bean
    public Binding orderPaymentFailedBinding() {
        return BindingBuilder.bind(orderPaymentFailedQueue())
            .to(orderExchange())
            .with(ROUTING_KEY_PAYMENT_FAILED);
    }

    /**
     * Binding for dispatch queue.
     */
    @Bean
    public Binding orderDispatchBinding() {
        return BindingBuilder.bind(orderDispatchQueue())
            .to(orderExchange())
            .with(ROUTING_KEY_DISPATCH);
    }

    /**
     * Binding for refund queue.
     */
    @Bean
    public Binding orderRefundBinding() {
        return BindingBuilder.bind(orderRefundQueue())
            .to(orderExchange())
            .with(ROUTING_KEY_REFUND);
    }

    /**
     * JSON message converter for RabbitMQ messages.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
