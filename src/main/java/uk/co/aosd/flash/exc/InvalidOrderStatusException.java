package uk.co.aosd.flash.exc;

import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.co.aosd.flash.domain.OrderStatus;

@Getter
@RequiredArgsConstructor
public class InvalidOrderStatusException extends RuntimeException {
    private final UUID orderId;
    private final OrderStatus currentStatus;
    private final OrderStatus requiredStatus;
    private final String operation;

    @Override
    public String getMessage() {
        return String.format("Invalid status transition: %s → %s for order %s", 
            currentStatus, requiredStatus, orderId);
    }
}
