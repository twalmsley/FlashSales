package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

/**
 * A client view of a product.
 */
@Schema(
    name = "ClientProduct",
    description = "Product representation returned to clients."
)
public record ClientProductDto(
    @Schema(description = "Product identifier.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    String id,
    @Schema(description = "Product name.", example = "Wireless Mouse")
    @NotEmpty(message = "A name must be provided.") String name,
    @Schema(description = "Product description.", example = "Ergonomic wireless mouse with USB-C charging.")
    @NotEmpty(message = "A description must be provided.") String description,
    @Schema(description = "Base price (non-sale price).", example = "29.99")
    @Min(value = 0, message = "Price cannot be negative") BigDecimal basePrice)
    implements Serializable {
}
