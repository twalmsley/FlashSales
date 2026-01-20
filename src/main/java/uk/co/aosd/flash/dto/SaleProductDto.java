package uk.co.aosd.flash.dto;

import java.io.Serializable;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

/**
 * A Sale Product DTO.
 */
public record SaleProductDto(
    @NotEmpty(message = "ID cannot be empty") String id,
    @Min(value = 0, message = "ReservedCount cannot be negative") Integer reservedCount)
    implements Serializable {
}
