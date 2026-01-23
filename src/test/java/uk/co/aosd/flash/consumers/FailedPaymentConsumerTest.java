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
 * Test the Failed Payment Consumer.
 */
public class FailedPaymentConsumerTest {

    private OrderService orderService;
    private FailedPaymentConsumer consumer;

    private UUID orderId;

    @BeforeEach
    public void beforeEach() {
        orderService = Mockito.mock(OrderService.class);
        consumer = new FailedPaymentConsumer(orderService);
        orderId = UUID.randomUUID();
    }

    @Test
    public void shouldProcessFailedPaymentSuccessfully() {
        // Given
        final String orderIdStr = orderId.toString();

        // When
        consumer.processFailedPayment(orderIdStr);

        // Then
        verify(orderService).processFailedPayment(orderId);
    }

    @Test
    public void shouldThrowExceptionWhenInvalidUUID() {
        // Given
        final String invalidOrderIdStr = "invalid-uuid";

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            consumer.processFailedPayment(invalidOrderIdStr);
        });

        verify(orderService, never()).processFailedPayment(any());
    }

    @Test
    public void shouldRethrowExceptionWhenOrderNotFound() {
        // Given
        final String orderIdStr = orderId.toString();
        doThrow(new OrderNotFoundException(orderId)).when(orderService).processFailedPayment(orderId);

        // When/Then
        assertThrows(OrderNotFoundException.class, () -> {
            consumer.processFailedPayment(orderIdStr);
        });

        verify(orderService).processFailedPayment(orderId);
    }

    @Test
    public void shouldRethrowExceptionWhenServiceThrowsRuntimeException() {
        // Given
        final String orderIdStr = orderId.toString();
        final RuntimeException runtimeException = new RuntimeException("Service error");
        doThrow(runtimeException).when(orderService).processFailedPayment(orderId);

        // When/Then
        assertThrows(RuntimeException.class, () -> {
            consumer.processFailedPayment(orderIdStr);
        });

        verify(orderService).processFailedPayment(orderId);
    }
}
