package uk.co.aosd.flash.services;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Mock payment service for processing payments.
 * Can be configured to always succeed, always fail, or randomly succeed/fail.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final Random random = new Random();

    @Value("${app.payment.always-succeed:false}")
    private boolean alwaysSucceed;

    @Value("${app.payment.always-fail:false}")
    private boolean alwaysFail;

    @Value("${app.payment.success-rate:0.9}")
    private double successRate;

    /**
     * Process a payment for an order.
     *
     * @param orderId the order ID
     * @param amount  the payment amount
     * @return true if payment succeeded, false otherwise
     */
    public boolean processPayment(final UUID orderId, final BigDecimal amount) {
        log.info("Processing payment for order {} with amount {}", orderId, amount);

        if (alwaysFail) {
            log.warn("Payment failed for order {} (always-fail mode)", orderId);
            return false;
        }

        if (alwaysSucceed) {
            log.info("Payment succeeded for order {} (always-succeed mode)", orderId);
            return true;
        }

        // Random success/fail based on success rate
        final boolean success = random.nextDouble() < successRate;
        if (success) {
            log.info("Payment succeeded for order {}", orderId);
        } else {
            log.warn("Payment failed for order {} (random failure)", orderId);
        }
        return success;
    }
}
