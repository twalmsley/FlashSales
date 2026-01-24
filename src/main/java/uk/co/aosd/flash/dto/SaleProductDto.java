package uk.co.aosd.flash.dto;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

/**
 * A Sale Product DTO.
 */
@Schema(
    name = "SaleProduct",
    description = "Product allocation inside a flash sale definition."
)
public record SaleProductDto(
    @Schema(description = "Product identifier.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    @NotEmpty(message = "ID cannot be empty") String id,
    @Schema(description = "Quantity reserved from product stock for this sale.", example = "20")
    @Min(value = 0, message = "ReservedCount cannot be negative") Integer reservedCount)
    implements Serializable {
}
