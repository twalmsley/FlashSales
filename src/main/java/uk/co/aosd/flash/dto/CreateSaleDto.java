package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import uk.co.aosd.flash.domain.SaleStatus;

public record CreateSaleDto(String id,
    @NotEmpty(message = "A non-empty title is needed for the sale.") String title,
    @NotNull(message = "The sale needs a valid start time.") LocalDateTime startTime,
    @NotNull(message = "The sale needs a valid end time.") LocalDateTime endTime,
    @NotNull(message = "The sale needs a valid status.") SaleStatus status,
    @NotEmpty(message = "The sale needs at least one product.") ProductDto[] products) implements Serializable {
}
