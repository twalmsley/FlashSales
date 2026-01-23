package uk.co.aosd.flash.controllers;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.CreateSaleDto;
import uk.co.aosd.flash.dto.FlashSaleResponseDto;
import uk.co.aosd.flash.dto.UpdateFlashSaleDto;
import uk.co.aosd.flash.exc.DuplicateEntityException;
import uk.co.aosd.flash.exc.FlashSaleNotFoundException;
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

    /**
     * Cancel a flash sale by ID.
     * Only DRAFT and ACTIVE sales can be cancelled.
     * Releases reserved stock back to products.
     *
     * @param id the flash sale ID to cancel
     * @return ResponseEntity with no content on success
     */
    @PostMapping("/flash_sale/{id}/cancel")
    public ResponseEntity<Void> cancelSale(@PathVariable final String id) {
        log.info("Cancelling Flash Sale: {}", id);
        final var uuid = UUID.fromString(id);
        service.cancelFlashSale(uuid);
        log.info("Cancelled Flash Sale: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * List all flash sales with optional filters.
     *
     * @param status optional status filter
     * @param startDate optional start date filter (ISO-8601 format)
     * @param endDate optional end date filter (ISO-8601 format)
     * @return ResponseEntity with list of flash sales
     */
    @GetMapping("/flash_sale")
    public ResponseEntity<List<FlashSaleResponseDto>> getAllFlashSales(
        @RequestParam(required = false) final String status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime endDate) {
        
        log.info("Getting all flash sales with filters: status={}, startDate={}, endDate={}", status, startDate, endDate);
        
        SaleStatus saleStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                saleStatus = SaleStatus.valueOf(status.toUpperCase());
            } catch (final IllegalArgumentException e) {
                log.warn("Invalid status value: {}", status);
                return ResponseEntity.badRequest().build();
            }
        }
        
        final List<FlashSaleResponseDto> sales = service.getAllFlashSales(saleStatus, startDate, endDate);
        log.info("Returned {} flash sale(s)", sales.size());
        return ResponseEntity.ok(sales);
    }

    /**
     * Get flash sale details by ID.
     *
     * @param id the flash sale ID
     * @return ResponseEntity with flash sale DTO, or 404 if not found
     */
    @GetMapping("/flash_sale/{id}")
    public ResponseEntity<FlashSaleResponseDto> getFlashSaleById(@PathVariable final String id) {
        log.info("Getting flash sale by ID: {}", id);
        
        try {
            final UUID uuid = UUID.fromString(id);
            final FlashSaleResponseDto sale = service.getFlashSaleById(uuid);
            log.info("Fetched flash sale with id: {}", id);
            return ResponseEntity.ok(sale);
        } catch (final IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", id);
            return ResponseEntity.badRequest().build();
        } catch (final FlashSaleNotFoundException e) {
            log.info("Flash sale not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Update a flash sale.
     *
     * @param id the flash sale ID
     * @param updateDto the update DTO
     * @return ResponseEntity with updated flash sale DTO, or 404 if not found, or 400 for validation errors
     */
    @PutMapping("/flash_sale/{id}")
    public ResponseEntity<FlashSaleResponseDto> updateFlashSale(
        @PathVariable final String id,
        @Valid @RequestBody final UpdateFlashSaleDto updateDto) {
        
        log.info("Updating flash sale: {} with {}", id, updateDto);
        
        try {
            final UUID uuid = UUID.fromString(id);
            final FlashSaleResponseDto updated = service.updateFlashSale(uuid, updateDto);
            log.info("Updated flash sale: {}", id);
            return ResponseEntity.ok(updated);
        } catch (final IllegalArgumentException e) {
            log.error("Invalid UUID format or validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (final FlashSaleNotFoundException e) {
            log.info("Flash sale not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (final InvalidSaleTimesException | SaleDurationTooShortException e) {
            log.warn("Validation error updating flash sale: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a flash sale (only DRAFT status allowed).
     * Releases reserved stock back to products.
     *
     * @param id the flash sale ID
     * @return ResponseEntity with no content on success, or 404 if not found, or 400 if not DRAFT status
     */
    @DeleteMapping("/flash_sale/{id}")
    public ResponseEntity<Void> deleteFlashSale(@PathVariable final String id) {
        log.info("Deleting flash sale: {}", id);
        
        try {
            final UUID uuid = UUID.fromString(id);
            service.deleteFlashSale(uuid);
            log.info("Deleted flash sale: {}", id);
            return ResponseEntity.noContent().build();
        } catch (final IllegalArgumentException e) {
            log.error("Invalid UUID format or status error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (final FlashSaleNotFoundException e) {
            log.info("Flash sale not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
