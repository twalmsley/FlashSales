package uk.co.aosd.flash.services;

import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import uk.co.aosd.flash.dto.CreateSaleDto;
import uk.co.aosd.flash.exc.DuplicateEntityException;
import uk.co.aosd.flash.exc.InvalidSaleTimesException;
import uk.co.aosd.flash.exc.SaleDurationTooShortException;

@Service
@RequiredArgsConstructor
public class FlashSalesService {

    @Value("${app.settings.min-sale-duration-minutes}")
    private long MIN_SALE_DURATION = 10;// Default to 10 minutes.

    @Transactional
    public UUID createFlashSale(@Valid final CreateSaleDto sale) throws DuplicateEntityException, SaleDurationTooShortException, InvalidSaleTimesException {
        if (!sale.startTime().isBefore(sale.endTime())) {
            throw new InvalidSaleTimesException(sale.startTime(), sale.endTime());
        }
        final var durationMinutes = (sale.endTime().toInstant(ZoneOffset.UTC).toEpochMilli() - sale.startTime().toInstant(ZoneOffset.UTC).toEpochMilli())
            / 60000;
        if (durationMinutes < MIN_SALE_DURATION) {
            throw new SaleDurationTooShortException("Sale duration of " + durationMinutes + " minutes is less than " + MIN_SALE_DURATION);
        }
        return UUID.randomUUID();
    }

}
