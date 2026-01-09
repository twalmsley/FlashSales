package uk.co.aosd.flash.dto;

import java.math.BigDecimal;

public record ProductDto(
    String id,
    String name,
    String description,
    Integer totalPhysicalStock,
    BigDecimal basePrice) {
}
