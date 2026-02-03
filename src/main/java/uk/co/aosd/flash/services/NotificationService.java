package uk.co.aosd.flash.services;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for sending notifications to users.
 * Currently uses a dummy email sender that logs notifications as INFO messages.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /**
     * Send order confirmation notification.
     *
     * @param userId  the user ID
     * @param orderId the order ID
     */
    public void sendOrderConfirmation(final UUID userId, final UUID orderId) {
        log.info("EMAIL NOTIFICATION: Order confirmation sent to user {} for order {}", userId, orderId);
    }

    /**
     * Send payment failed notification.
     *
     * @param userId  the user ID
     * @param orderId the order ID
     */
    public void sendPaymentFailedNotification(final UUID userId, final UUID orderId) {
        log.info("EMAIL NOTIFICATION: Payment failed notification sent to user {} for order {}", userId, orderId);
    }

    /**
     * Send refund notification.
     *
     * @param userId  the user ID
     * @param orderId the order ID
     */
    public void sendRefundNotification(final UUID userId, final UUID orderId) {
        log.info("EMAIL NOTIFICATION: Refund notification sent to user {} for order {}", userId, orderId);
    }

    /**
     * Send dispatch notification.
     *
     * @param userId  the user ID
     * @param orderId the order ID
     */
    public void sendDispatchNotification(final UUID userId, final UUID orderId) {
        log.info("EMAIL NOTIFICATION: Dispatch notification sent to user {} for order {}", userId, orderId);
    }

    /**
     * Send cancellation notification (user cancelled a PENDING order).
     *
     * @param userId  the user ID
     * @param orderId the order ID
     */
    public void sendCancellationNotification(final UUID userId, final UUID orderId) {
        log.info("EMAIL NOTIFICATION: Order cancelled notification sent to user {} for order {}", userId, orderId);
    }
}
