package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

import uk.co.aosd.flash.domain.SaleStatus;

/**
 * DTO for Flash Sale responses.
 */
public record FlashSaleResponseDto(
    String id,
    String title,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    SaleStatus status,
    List<FlashSaleItemDto> items)
    implements Serializable {
}
