package uk.co.aosd.flash.controllers;

import java.net.URI;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.co.aosd.flash.dto.CreateSaleDto;
import uk.co.aosd.flash.exc.DuplicateEntityException;
import uk.co.aosd.flash.exc.InvalidSaleTimesException;
import uk.co.aosd.flash.exc.SaleDurationTooShortException;
import uk.co.aosd.flash.services.FlashSalesService;

/**
 * A REST API for Flash Sales.
 */
@RestController
@Profile("admin-service")
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class FlashSaleAdminRestApi {

    private static Logger log = LoggerFactory.getLogger(FlashSaleAdminRestApi.class.getName());

    private final FlashSalesService service;

    /**
     * Create a new Flash Sale.
     *
     * @param sale CreateSaleDto
     * @return ResponseEntity with a Stringified UUID.
     * @throws DuplicateEntityException
     *             if the database reports a duplicate.
     * @throws SaleDurationTooShortException
     *             if the sale is too short.
     * @throws InvalidSaleTimesException
     *             if the start time is after the end time.
     */
    @PostMapping("/flash_sale")
    public ResponseEntity<String> createSale(@Valid @RequestBody final CreateSaleDto sale)
        throws DuplicateEntityException, SaleDurationTooShortException, InvalidSaleTimesException {

        log.info("Creating Flash Sale: " + sale);
        final var uuid = service.createFlashSale(sale);
        log.info("Created Flash Sale: " + uuid);

        return ResponseEntity.created(URI.create("/api/v1/admin/flash_sale/" + uuid.toString())).build();
    }
}
