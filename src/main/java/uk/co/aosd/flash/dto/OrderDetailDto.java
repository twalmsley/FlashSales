package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import uk.co.aosd.flash.domain.OrderStatus;

/**
 * DTO for detailed order information in client API responses.
 */
@Schema(
    name = "OrderDetail",
    description = "Detailed view of an order for a given user."
)
public record OrderDetailDto(
    @Schema(description = "Order identifier.", example = "2b8efb9f-6f89-4b2d-8c73-4b2f9d4d2e1a")
    UUID orderId,
    @Schema(description = "User identifier that owns the order.", example = "9b2b8c2c-2f53-4a57-a07e-0a2b2b1de3a9")
    UUID userId,
    @Schema(description = "Product identifier.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID productId,
    @Schema(description = "Product name.", example = "Wireless Mouse")
    String productName,
    @Schema(description = "Flash sale item identifier.", example = "b1b7a3c0-8d3b-4d10-8cc1-3c5f88f4bb5a")
    UUID flashSaleItemId,
    @Schema(description = "Flash sale identifier.", example = "5b3c3f18-2f88-4c38-8b35-9aa6d9b9f5af")
    UUID flashSaleId,
    @Schema(description = "Flash sale title.", example = "Winter Deals")
    String flashSaleTitle,
    @Schema(description = "Unit price paid per item.", example = "19.99")
    BigDecimal soldPrice,
    @Schema(description = "Quantity purchased.", example = "2")
    Integer soldQuantity,
    @Schema(description = "Total amount paid for this order.", example = "39.98")
    BigDecimal totalAmount,
    @Schema(description = "Current order status.")
    OrderStatus status,
    @Schema(description = "Order creation timestamp (ISO-8601).", example = "2026-01-24T10:15:30Z")
    OffsetDateTime createdAt)
    implements Serializable {
}
