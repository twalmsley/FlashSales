package uk.co.aosd.flash.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "SaleStatus",
    description = "Lifecycle status of a flash sale."
)
public enum SaleStatus {
    DRAFT, ACTIVE, COMPLETED, CANCELLED
}
