package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.util.UUID;

import uk.co.aosd.flash.domain.OrderStatus;

/**
 * DTO for order response.
 */
public record OrderResponseDto(
    UUID orderId,
    OrderStatus status,
    String message)
    implements Serializable {
}
