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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.AddFlashSaleItemDto;
import uk.co.aosd.flash.dto.CreateSaleDto;
import uk.co.aosd.flash.dto.ErrorResponseDto;
import uk.co.aosd.flash.dto.FlashSaleResponseDto;
import uk.co.aosd.flash.dto.UpdateFlashSaleDto;
import uk.co.aosd.flash.dto.UpdateFlashSaleItemDto;
import uk.co.aosd.flash.exc.DuplicateEntityException;
import uk.co.aosd.flash.exc.FlashSaleItemNotFoundException;
import uk.co.aosd.flash.exc.FlashSaleNotFoundException;
import uk.co.aosd.flash.exc.InsufficientResourcesException;
import uk.co.aosd.flash.exc.InvalidSaleTimesException;
import uk.co.aosd.flash.exc.ProductNotFoundException;
import uk.co.aosd.flash.exc.SaleDurationTooShortException;
import uk.co.aosd.flash.services.FlashSalesService;

/**
 * A REST API for Flash Sales.
 */
@RestController
@Profile("admin-service")
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(
    name = "Flash Sales (Admin)",
    description = "Admin endpoints for creating and managing flash sales and their items."
)
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
    @Operation(
        summary = "Create flash sale",
        description = "Creates a new flash sale and returns a Location header pointing to the created resource."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Flash sale created.",
            headers = @Header(
                name = "Location",
                description = "URI of the created flash sale resource.",
                schema = @Schema(type = "string")
            ),
            content = @Content
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Duplicate flash sale.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Validation error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid sale times/duration or other invalid input.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
    })
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
    @Operation(
        summary = "Cancel flash sale",
        description = "Cancels a flash sale by id. Only DRAFT and ACTIVE sales can be cancelled."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Flash sale cancelled.", content = @Content),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid id or cancellation not permitted.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Flash sale not found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
    })
    public ResponseEntity<Void> cancelSale(
        @Parameter(description = "Flash sale identifier (UUID).", example = "5b3c3f18-2f88-4c38-8b35-9aa6d9b9f5af")
        @PathVariable final String id) {
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
     * @param startDate optional filter window start (ISO-8601 format). When provided, only sales whose time period
     *                  overlaps the specified window are returned.
     * @param endDate optional filter window end (ISO-8601 format). When provided, only sales whose time period
     *                overlaps the specified window are returned.
     * @return ResponseEntity with list of flash sales
     */
    @GetMapping("/flash_sale")
    @Operation(
        summary = "List flash sales",
        description = "Lists flash sales with optional status and date range filters."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of flash sales.",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = FlashSaleResponseDto.class))
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid status or date filters.", content = @Content)
    })
    public ResponseEntity<List<FlashSaleResponseDto>> getAllFlashSales(
        @Parameter(description = "Optional status filter.", schema = @Schema(implementation = SaleStatus.class), example = "DRAFT")
        @RequestParam(required = false) final String status,
        @Parameter(description = "Optional filter window start (ISO-8601). Returns sales whose time period overlaps the window.", example = "2026-01-01T00:00:00Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime startDate,
        @Parameter(description = "Optional filter window end (ISO-8601). Returns sales whose time period overlaps the window.", example = "2026-12-31T23:59:59Z")
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
    @Operation(summary = "Get flash sale by id", description = "Returns flash sale details by id.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Flash sale found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = FlashSaleResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid UUID format.", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Flash sale not found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
    })
    public ResponseEntity<FlashSaleResponseDto> getFlashSaleById(
        @Parameter(description = "Flash sale identifier (UUID).", example = "5b3c3f18-2f88-4c38-8b35-9aa6d9b9f5af")
        @PathVariable final String id) {
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
    @Operation(summary = "Update flash sale", description = "Updates a flash sale by id.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Flash sale updated.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = FlashSaleResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid input or invalid UUID format.", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Flash sale not found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
    })
    public ResponseEntity<FlashSaleResponseDto> updateFlashSale(
        @Parameter(description = "Flash sale identifier (UUID).", example = "5b3c3f18-2f88-4c38-8b35-9aa6d9b9f5af")
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
    @Operation(
        summary = "Delete flash sale",
        description = "Deletes a flash sale by id (only DRAFT status allowed)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Flash sale deleted.", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid id or status error.", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Flash sale not found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
    })
    public ResponseEntity<Void> deleteFlashSale(
        @Parameter(description = "Flash sale identifier (UUID).", example = "5b3c3f18-2f88-4c38-8b35-9aa6d9b9f5af")
        @PathVariable final String id) {
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

    /**
     * Add items to an existing flash sale.
     * Only DRAFT sales can have items added.
     *
     * @param id    the flash sale ID
     * @param items the items to add
     * @return ResponseEntity with updated flash sale DTO, or 404 if not found, or 400 for validation errors, or 409 for duplicate products
     */
    @PostMapping("/flash_sale/{id}/items")
    @Operation(
        summary = "Add flash sale items",
        description = "Adds item(s) to a DRAFT flash sale."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Items added.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = FlashSaleResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid input / insufficient resources.", content = @Content),
        @ApiResponse(responseCode = "404", description = "Flash sale not found.", content = @Content),
        @ApiResponse(
            responseCode = "409",
            description = "Duplicate product already in sale.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Validation error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
    })
    public ResponseEntity<FlashSaleResponseDto> addItemsToFlashSale(
        @Parameter(description = "Flash sale identifier (UUID).", example = "5b3c3f18-2f88-4c38-8b35-9aa6d9b9f5af")
        @PathVariable final String id,
        @Valid @RequestBody final List<AddFlashSaleItemDto> items) {
        
        log.info("Adding items to flash sale: {}", id);
        
        try {
            final UUID uuid = UUID.fromString(id);
            final FlashSaleResponseDto updated = service.addItemsToFlashSale(uuid, items);
            log.info("Added items to flash sale: {}", id);
            return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/admin/flash_sale/" + uuid.toString()))
                .body(updated);
        } catch (final IllegalArgumentException e) {
            log.error("Invalid UUID format or validation error: {}", e.getMessage());
            // Check if it's a duplicate product error
            if (e.getMessage() != null && e.getMessage().contains("already in sale")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            return ResponseEntity.badRequest().build();
        } catch (final FlashSaleNotFoundException e) {
            log.info("Flash sale not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (final ProductNotFoundException e) {
            log.warn("Product not found: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (final InsufficientResourcesException e) {
            log.warn("Insufficient resources: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update a flash sale item.
     * Only DRAFT sales can have items updated.
     *
     * @param id      the flash sale ID
     * @param itemId   the flash sale item ID
     * @param updateDto the update DTO
     * @return ResponseEntity with updated flash sale DTO, or 404 if not found, or 400 for validation errors
     */
    @PutMapping("/flash_sale/{id}/items/{itemId}")
    @Operation(
        summary = "Update flash sale item",
        description = "Updates a flash sale item inside a DRAFT sale."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Item updated.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = FlashSaleResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid UUID or invalid input.", content = @Content),
        @ApiResponse(responseCode = "404", description = "Sale or item not found.", content = @Content),
        @ApiResponse(
            responseCode = "422",
            description = "Validation error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
    })
    public ResponseEntity<FlashSaleResponseDto> updateFlashSaleItem(
        @Parameter(description = "Flash sale identifier (UUID).", example = "5b3c3f18-2f88-4c38-8b35-9aa6d9b9f5af")
        @PathVariable final String id,
        @Parameter(description = "Flash sale item identifier (UUID).", example = "b1b7a3c0-8d3b-4d10-8cc1-3c5f88f4bb5a")
        @PathVariable final String itemId,
        @Valid @RequestBody final UpdateFlashSaleItemDto updateDto) {
        
        log.info("Updating flash sale item: {} in sale {}", itemId, id);
        
        try {
            final UUID saleUuid = UUID.fromString(id);
            final UUID itemUuid = UUID.fromString(itemId);
            final FlashSaleResponseDto updated = service.updateFlashSaleItem(saleUuid, itemUuid, updateDto);
            log.info("Updated flash sale item: {} in sale {}", itemId, id);
            return ResponseEntity.ok(updated);
        } catch (final IllegalArgumentException e) {
            log.error("Invalid UUID format or validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (final FlashSaleNotFoundException e) {
            log.info("Flash sale not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (final FlashSaleItemNotFoundException e) {
            log.info("Flash sale item not found with id: {} in sale {}", itemId, id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (final InsufficientResourcesException e) {
            log.warn("Insufficient resources: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Remove a flash sale item from a sale.
     * Only DRAFT sales can have items removed.
     *
     * @param id     the flash sale ID
     * @param itemId the flash sale item ID
     * @return ResponseEntity with no content on success, or 404 if not found, or 400 for status errors
     */
    @DeleteMapping("/flash_sale/{id}/items/{itemId}")
    @Operation(
        summary = "Remove flash sale item",
        description = "Removes a flash sale item from a DRAFT sale."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Item removed.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = FlashSaleResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid UUID or status error.", content = @Content),
        @ApiResponse(responseCode = "404", description = "Sale or item not found.", content = @Content)
    })
    public ResponseEntity<FlashSaleResponseDto> removeFlashSaleItem(
        @Parameter(description = "Flash sale identifier (UUID).", example = "5b3c3f18-2f88-4c38-8b35-9aa6d9b9f5af")
        @PathVariable final String id,
        @Parameter(description = "Flash sale item identifier (UUID).", example = "b1b7a3c0-8d3b-4d10-8cc1-3c5f88f4bb5a")
        @PathVariable final String itemId) {
        
        log.info("Removing flash sale item: {} from sale {}", itemId, id);
        
        try {
            final UUID saleUuid = UUID.fromString(id);
            final UUID itemUuid = UUID.fromString(itemId);
            final FlashSaleResponseDto updated = service.removeFlashSaleItem(saleUuid, itemUuid);
            log.info("Removed flash sale item: {} from sale {}", itemId, id);
            return ResponseEntity.ok(updated);
        } catch (final IllegalArgumentException e) {
            log.error("Invalid UUID format or status error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (final FlashSaleNotFoundException e) {
            log.info("Flash sale not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (final FlashSaleItemNotFoundException e) {
            log.info("Flash sale item not found with id: {} in sale {}", itemId, id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
