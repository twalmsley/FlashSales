package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import uk.co.aosd.flash.domain.SaleStatus;

/**
 * DTO for creating a new Flash Sale.
 */
public record CreateSaleDto(String id,
    @NotEmpty(message = "A non-empty title is needed for the sale.") String title,
    @NotNull(message = "The sale needs a valid start time.") OffsetDateTime startTime,
    @NotNull(message = "The sale needs a valid end time.") OffsetDateTime endTime,
    @NotNull(message = "The sale needs a valid status.") SaleStatus status,
    @Valid @NotEmpty(message = "The sale needs at least one product.") List<SaleProductDto> products) implements Serializable {
}
