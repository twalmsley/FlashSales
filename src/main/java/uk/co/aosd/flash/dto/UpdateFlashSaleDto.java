package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * DTO for updating a Flash Sale.
 * All fields are optional.
 */
public record UpdateFlashSaleDto(
    String title,
    OffsetDateTime startTime,
    OffsetDateTime endTime)
    implements Serializable {
}
