package uk.co.aosd.flash.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

/**
 * Test the ClientActiveSaleDto.
 */
public class ClientActiveSaleDtoTest {

    @Test
    public void shouldReturnRemainingTimeWhenSaleIsActive() {
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = startTime.plusHours(2);
        final OffsetDateTime currentTime = startTime.plusMinutes(30);

        final ClientActiveSaleDto dto = new ClientActiveSaleDto(
            "sale-1",
            "item-1",
            "Test Sale",
            startTime,
            endTime,
            "product-1",
            "Product Name",
            "Product description",
            BigDecimal.valueOf(109.99),
            100,
            10,
            BigDecimal.valueOf(99.99));

        final Duration remaining = dto.getRemainingTime(currentTime);

        assertFalse(remaining.isZero());
        assertFalse(remaining.isNegative());
        assertEquals(Duration.ofMinutes(90), remaining);
    }

    @Test
    public void shouldReturnZeroWhenSaleHasEnded() {
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = startTime.plusHours(2);
        final OffsetDateTime currentTime = endTime.plusMinutes(30);

        final ClientActiveSaleDto dto = new ClientActiveSaleDto(
            "sale-1",
            "item-1",
            "Test Sale",
            startTime,
            endTime,
            "product-1",
            "Product Name",
            "Product description",
            BigDecimal.valueOf(109.99),
            100,
            10,
            BigDecimal.valueOf(99.99));

        final Duration remaining = dto.getRemainingTime(currentTime);

        assertEquals(Duration.ZERO, remaining);
    }

    @Test
    public void shouldReturnZeroWhenCurrentTimeEqualsEndTime() {
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = startTime.plusHours(2);
        final OffsetDateTime currentTime = endTime;

        final ClientActiveSaleDto dto = new ClientActiveSaleDto(
            "sale-1",
            "item-1",
            "Test Sale",
            startTime,
            endTime,
            "product-1",
            "Product Name",
            "Product description",
            BigDecimal.valueOf(109.99),
            100,
            10,
            BigDecimal.valueOf(99.99));

        final Duration remaining = dto.getRemainingTime(currentTime);

        assertEquals(Duration.ZERO, remaining);
    }

    @Test
    public void shouldReturnCorrectRemainingTimeForDifferentDurations() {
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = startTime.plusDays(1).plusHours(3).plusMinutes(15);
        final OffsetDateTime currentTime = startTime.plusHours(6);

        final ClientActiveSaleDto dto = new ClientActiveSaleDto(
            "sale-1",
            "item-1",
            "Test Sale",
            startTime,
            endTime,
            "product-1",
            "Product Name",
            "Product description",
            BigDecimal.valueOf(109.99),
            100,
            10,
            BigDecimal.valueOf(99.99));

        final Duration remaining = dto.getRemainingTime(currentTime);

        // Expected: 1 day + 3 hours + 15 minutes - 6 hours = 21 hours + 15 minutes
        assertEquals(Duration.ofHours(21).plusMinutes(15), remaining);
    }
}
