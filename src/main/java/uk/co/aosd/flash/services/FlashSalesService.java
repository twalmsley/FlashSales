package uk.co.aosd.flash.services;

import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.co.aosd.flash.domain.FlashSale;
import uk.co.aosd.flash.dto.CreateSaleDto;
import uk.co.aosd.flash.exc.DuplicateEntityException;
import uk.co.aosd.flash.exc.InvalidSaleTimesException;
import uk.co.aosd.flash.exc.SaleDurationTooShortException;
import uk.co.aosd.flash.repository.FlashSaleRepository;

/**
 * A Service for working with Flash Sales.
 */
@Service
@RequiredArgsConstructor
public class FlashSalesService {

    private final FlashSaleRepository repository;

    @Value("${app.settings.min-sale-duration-minutes}")
    private long minSaleDuration = 10; // Default to 10 minutes.

    /**
     * Create a new Flash Sale.
     *
     * @param sale
     *            CreateSaleDto
     * @return UUID
     * @throws DuplicateEntityException
     *             if the database reports a duplicate.
     * @throws SaleDurationTooShortException
     *             if the sale is too short.
     * @throws InvalidSaleTimesException
     *             if the start time is after the end time.
     */
    @Transactional
    public UUID createFlashSale(@Valid final CreateSaleDto sale) throws DuplicateEntityException, SaleDurationTooShortException, InvalidSaleTimesException {
        if (!sale.startTime().isBefore(sale.endTime())) {
            throw new InvalidSaleTimesException(sale.startTime(), sale.endTime());
        }
        final var durationMinutes = (sale.endTime().toInstant().toEpochMilli() - sale.startTime().toInstant().toEpochMilli())
            / 60000;
        if (durationMinutes < minSaleDuration) {
            throw new SaleDurationTooShortException("Sale duration of " + durationMinutes + " minutes is less than " + minSaleDuration);
        }

        final FlashSale s = new FlashSale(null, sale.title(), sale.startTime(), sale.endTime(), sale.status());
        final var saved = repository.save(s);
        return saved.getId();
    }

}
