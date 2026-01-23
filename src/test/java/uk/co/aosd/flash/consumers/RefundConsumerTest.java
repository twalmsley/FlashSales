package uk.co.aosd.flash.consumers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.co.aosd.flash.domain.Order;
import uk.co.aosd.flash.domain.OrderStatus;
import uk.co.aosd.flash.repository.OrderRepository;
import uk.co.aosd.flash.services.NotificationService;

/**
 * Test the Refund Consumer.
 */
public class RefundConsumerTest {

    private OrderRepository orderRepository;
    private NotificationService notificationService;
    private RefundConsumer consumer;

    private UUID orderId;
    private UUID userId;
    private Order order;

    @BeforeEach
    public void beforeEach() {
        orderRepository = Mockito.mock(OrderRepository.class);
        notificationService = Mockito.mock(NotificationService.class);
        consumer = new RefundConsumer(orderRepository, notificationService);

        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();

        order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setStatus(OrderStatus.REFUNDED);
    }

    @Test
    public void shouldProcessRefundNotificationSuccessfully() {
        // Given
        final String orderIdStr = orderId.toString();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        consumer.processRefundNotification(orderIdStr);

        // Then
        verify(orderRepository).findById(orderId);
        verify(notificationService).sendRefundNotification(userId, orderId);
    }

    @Test
    public void shouldThrowExceptionWhenInvalidUUID() {
        // Given
        final String invalidOrderIdStr = "invalid-uuid";

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            consumer.processRefundNotification(invalidOrderIdStr);
        });

        verify(orderRepository, never()).findById(any());
        verify(notificationService, never()).sendRefundNotification(any(), any());
    }

    @Test
    public void shouldRethrowExceptionWhenOrderNotFound() {
        // Given
        final String orderIdStr = orderId.toString();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(RuntimeException.class, () -> {
            consumer.processRefundNotification(orderIdStr);
        });

        verify(orderRepository).findById(orderId);
        verify(notificationService, never()).sendRefundNotification(any(), any());
    }

    @Test
    public void shouldRethrowExceptionWhenRepositoryThrowsException() {
        // Given
        final String orderIdStr = orderId.toString();
        final RuntimeException repositoryException = new RuntimeException("Repository error");
        when(orderRepository.findById(orderId)).thenThrow(repositoryException);

        // When/Then
        assertThrows(RuntimeException.class, () -> {
            consumer.processRefundNotification(orderIdStr);
        });

        verify(orderRepository).findById(orderId);
        verify(notificationService, never()).sendRefundNotification(any(), any());
    }

    @Test
    public void shouldRethrowExceptionWhenNotificationServiceThrowsException() {
        // Given
        final String orderIdStr = orderId.toString();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        final RuntimeException notificationException = new RuntimeException("Notification error");
        Mockito.doThrow(notificationException).when(notificationService)
            .sendRefundNotification(userId, orderId);

        // When/Then
        assertThrows(RuntimeException.class, () -> {
            consumer.processRefundNotification(orderIdStr);
        });

        verify(orderRepository).findById(orderId);
        verify(notificationService).sendRefundNotification(userId, orderId);
    }
}
