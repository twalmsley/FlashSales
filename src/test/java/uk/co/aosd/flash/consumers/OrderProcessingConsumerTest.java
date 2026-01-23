package uk.co.aosd.flash.consumers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.co.aosd.flash.exc.OrderNotFoundException;
import uk.co.aosd.flash.services.OrderService;

/**
 * Test the Order Processing Consumer.
 */
public class OrderProcessingConsumerTest {

    private OrderService orderService;
    private OrderProcessingConsumer consumer;

    private UUID orderId;

    @BeforeEach
    public void beforeEach() {
        orderService = Mockito.mock(OrderService.class);
        consumer = new OrderProcessingConsumer(orderService);
        orderId = UUID.randomUUID();
    }

    @Test
    public void shouldProcessOrderSuccessfully() {
        // Given
        final String orderIdStr = orderId.toString();

        // When
        consumer.processOrder(orderIdStr);

        // Then
        verify(orderService).processOrderPayment(orderId);
    }

    @Test
    public void shouldThrowExceptionWhenInvalidUUID() {
        // Given
        final String invalidOrderIdStr = "invalid-uuid";

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            consumer.processOrder(invalidOrderIdStr);
        });

        verify(orderService, never()).processOrderPayment(any());
    }

    @Test
    public void shouldRethrowExceptionWhenOrderNotFound() {
        // Given
        final String orderIdStr = orderId.toString();
        doThrow(new OrderNotFoundException(orderId)).when(orderService).processOrderPayment(orderId);

        // When/Then
        assertThrows(OrderNotFoundException.class, () -> {
            consumer.processOrder(orderIdStr);
        });

        verify(orderService).processOrderPayment(orderId);
    }

    @Test
    public void shouldRethrowExceptionWhenServiceThrowsRuntimeException() {
        // Given
        final String orderIdStr = orderId.toString();
        final RuntimeException runtimeException = new RuntimeException("Service error");
        doThrow(runtimeException).when(orderService).processOrderPayment(orderId);

        // When/Then
        assertThrows(RuntimeException.class, () -> {
            consumer.processOrder(orderIdStr);
        });

        verify(orderService).processOrderPayment(orderId);
    }
}
