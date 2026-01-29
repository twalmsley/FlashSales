package uk.co.aosd.flash.consumers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.co.aosd.flash.dto.ProcessPaymentResult;
import uk.co.aosd.flash.exc.OrderNotFoundException;
import uk.co.aosd.flash.services.OrderMessageSender;
import uk.co.aosd.flash.services.OrderService;

/**
 * Test the Order Processing Consumer.
 */
public class OrderProcessingConsumerTest {

    private OrderService orderService;
    private OrderMessageSender orderMessageSender;
    private OrderProcessingConsumer consumer;

    private UUID orderId;

    @BeforeEach
    public void beforeEach() {
        orderService = Mockito.mock(OrderService.class);
        orderMessageSender = Mockito.mock(OrderMessageSender.class);
        consumer = new OrderProcessingConsumer(orderService, orderMessageSender);
        orderId = UUID.randomUUID();
    }

    @Test
    public void shouldProcessOrderSuccessfullyAndSendForDispatchWhenPaymentSucceeds() {
        // Given
        final String orderIdStr = orderId.toString();
        when(orderService.processOrderPayment(orderId)).thenReturn(new ProcessPaymentResult(true, orderId));

        // When
        consumer.processOrder(orderIdStr);

        // Then
        verify(orderService).processOrderPayment(orderId);
        verify(orderMessageSender).sendForDispatch(orderId);
        verify(orderMessageSender, never()).sendForPaymentFailed(any());
    }

    @Test
    public void shouldProcessOrderAndSendForPaymentFailedWhenPaymentFails() {
        // Given
        final String orderIdStr = orderId.toString();
        when(orderService.processOrderPayment(orderId)).thenReturn(new ProcessPaymentResult(false, orderId));

        // When
        consumer.processOrder(orderIdStr);

        // Then
        verify(orderService).processOrderPayment(orderId);
        verify(orderMessageSender).sendForPaymentFailed(orderId);
        verify(orderMessageSender, never()).sendForDispatch(any());
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
        verify(orderMessageSender, never()).sendForDispatch(any());
        verify(orderMessageSender, never()).sendForPaymentFailed(any());
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
        verify(orderMessageSender, never()).sendForDispatch(any());
        verify(orderMessageSender, never()).sendForPaymentFailed(any());
    }
}
