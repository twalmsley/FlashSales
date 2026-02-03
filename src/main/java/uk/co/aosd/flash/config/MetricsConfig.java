package uk.co.aosd.flash.config;

import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * Configures custom metrics for the Flash Sales application: RabbitMQ queue
 * depth gauges for order queues. Cache metrics (gets, puts, evictions) are
 * auto-configured by Spring Boot when Micrometer and a cache implementation
 * are present.
 */
@Configuration
public class MetricsConfig {

    private static final List<String> ORDER_QUEUE_NAMES = List.of(
        RabbitMQConfig.ORDER_PROCESSING_QUEUE,
        RabbitMQConfig.ORDER_PAYMENT_FAILED_QUEUE,
        RabbitMQConfig.ORDER_DISPATCH_QUEUE,
        RabbitMQConfig.ORDER_REFUND_QUEUE
    );

    /**
     * Registers queue depth gauges for order-related RabbitMQ queues when
     * RabbitAdmin is available.
     */
    @Bean
    @ConditionalOnBean(RabbitAdmin.class)
    public RabbitQueueDepthMetrics rabbitQueueDepthMetrics(
            final MeterRegistry registry,
            final RabbitAdmin rabbitAdmin) {
        return new RabbitQueueDepthMetrics(registry, rabbitAdmin);
    }

    /**
     * Publishes RabbitMQ queue depth as gauges for order queues.
     */
    public static class RabbitQueueDepthMetrics {

        private final MeterRegistry registry;
        private final RabbitAdmin rabbitAdmin;

        public RabbitQueueDepthMetrics(final MeterRegistry registry, final RabbitAdmin rabbitAdmin) {
            this.registry = registry;
            this.rabbitAdmin = rabbitAdmin;
            registerGauges();
        }

        private void registerGauges() {
            for (String queueName : ORDER_QUEUE_NAMES) {
                final String name = queueName;
                registry.gauge("flash.rabbitmq.queue.depth", Tags.of("queue", name), this, m -> m.getQueueDepth(name));
            }
        }

        private int getQueueDepth(final String queueName) {
            try {
                java.util.Properties props = rabbitAdmin.getQueueProperties(queueName);
                if (props == null) {
                    return 0;
                }
                String count = props.getProperty("QUEUE_MESSAGE_COUNT", "0");
                return Integer.parseInt(count);
            } catch (Exception e) {
                return 0;
            }
        }
    }
}
