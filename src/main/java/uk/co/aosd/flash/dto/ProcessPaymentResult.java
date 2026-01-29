package uk.co.aosd.flash.dto;

import java.util.UUID;

/**
 * Result of processing an order payment.
 * Used by callers (e.g. OrderProcessingConsumer) to decide whether to send
 * dispatch or payment-failed message.
 */
public record ProcessPaymentResult(boolean success, UUID orderId) {
}
