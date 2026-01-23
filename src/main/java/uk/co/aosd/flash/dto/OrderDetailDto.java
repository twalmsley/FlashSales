package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import uk.co.aosd.flash.domain.OrderStatus;

/**
 * DTO for detailed order information in client API responses.
 */
public record OrderDetailDto(
    UUID orderId,
    UUID userId,
    UUID productId,
    String productName,
    UUID flashSaleItemId,
    UUID flashSaleId,
    String flashSaleTitle,
    BigDecimal soldPrice,
    Integer soldQuantity,
    BigDecimal totalAmount,
    OrderStatus status,
    OffsetDateTime createdAt)
    implements Serializable {
}
