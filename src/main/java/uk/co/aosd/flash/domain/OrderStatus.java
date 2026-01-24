package uk.co.aosd.flash.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "OrderStatus",
    description = "Lifecycle status of an order."
)
public enum OrderStatus {
    PENDING, PAID, FAILED, REFUNDED, DISPATCHED
}
