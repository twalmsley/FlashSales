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
import uk.co.aosd.flash.exc.InvalidOrderStatusException;
import uk.co.aosd.flash.exc.OrderNotFoundException;
import uk.co.aosd.flash.services.OrderService;

/**
 * Test the Dispatch Consumer.
 */
public class DispatchConsumerTest {

    private OrderService orderService;
    private DispatchConsumer consumer;

    private UUID orderId;

    @BeforeEach
    public void beforeEach() {
        orderService = Mockito.mock(OrderService.class);
        consumer = new DispatchConsumer(orderService);
        orderId = UUID.randomUUID();
    }

    @Test
    public void shouldProcessDispatchSuccessfully() {
        // Given
        final String orderIdStr = orderId.toString();

        // When
        consumer.processDispatch(orderIdStr);

        // Then
        verify(orderService).processDispatch(orderId);
    }

    @Test
    public void shouldThrowExceptionWhenInvalidUUID() {
        // Given
        final String invalidOrderIdStr = "invalid-uuid";

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            consumer.processDispatch(invalidOrderIdStr);
        });

        verify(orderService, never()).processDispatch(any());
    }

    @Test
    public void shouldRethrowExceptionWhenOrderNotFound() {
        // Given
        final String orderIdStr = orderId.toString();
        doThrow(new OrderNotFoundException(orderId)).when(orderService).processDispatch(orderId);

        // When/Then
        assertThrows(OrderNotFoundException.class, () -> {
            consumer.processDispatch(orderIdStr);
        });

        verify(orderService).processDispatch(orderId);
    }

    @Test
    public void shouldRethrowExceptionWhenInvalidOrderStatus() {
        // Given
        final String orderIdStr = orderId.toString();
        doThrow(new InvalidOrderStatusException(orderId, null, null, "dispatch"))
            .when(orderService).processDispatch(orderId);

        // When/Then
        assertThrows(InvalidOrderStatusException.class, () -> {
            consumer.processDispatch(orderIdStr);
        });

        verify(orderService).processDispatch(orderId);
    }

    @Test
    public void shouldRethrowExceptionWhenServiceThrowsRuntimeException() {
        // Given
        final String orderIdStr = orderId.toString();
        final RuntimeException runtimeException = new RuntimeException("Service error");
        doThrow(runtimeException).when(orderService).processDispatch(orderId);

        // When/Then
        assertThrows(RuntimeException.class, () -> {
            consumer.processDispatch(orderIdStr);
        });

        verify(orderService).processDispatch(orderId);
    }
}
