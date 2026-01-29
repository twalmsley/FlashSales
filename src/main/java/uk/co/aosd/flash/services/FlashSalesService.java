package uk.co.aosd.flash.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import uk.co.aosd.flash.domain.FlashSale;
import uk.co.aosd.flash.domain.FlashSaleItem;
import uk.co.aosd.flash.domain.Product;
import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.AddFlashSaleItemDto;
import uk.co.aosd.flash.dto.CreateSaleDto;
import uk.co.aosd.flash.dto.FlashSaleItemDto;
import uk.co.aosd.flash.dto.SaleProductDto;
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
import uk.co.aosd.flash.repository.FlashSaleItemRepository;
import uk.co.aosd.flash.repository.FlashSaleRepository;
import uk.co.aosd.flash.repository.ProductRepository;

/**
 * A Service for working with Flash Sales.
 */
@Service
@RequiredArgsConstructor
public class FlashSalesService {

    private static final Logger log = LoggerFactory.getLogger(FlashSalesService.class);

    private final FlashSaleRepository sales;

    private final FlashSaleItemRepository items;

    private final ProductRepository products;

    @Value("${app.settings.min-sale-duration-minutes}")
    private float minSaleDuration = 10; // Default to 10 minutes.

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
     * @throws ProductNotFoundException
     *             if any of the products cannot be found.
     * @throws InsufficientResourcesException
     *             if there is not enough stock to reserve for a product.
     */
    @Transactional
    @CacheEvict(value = {"flashSales", "activeSales", "draftSales"}, allEntries = true)
    public UUID createFlashSale(@Valid final CreateSaleDto sale) {
        log.info("Creating FlashSale: " + sale);
        if (!sale.startTime().isBefore(sale.endTime())) {
            log.error("Failed to create FlashSale due to start-after-end: " + sale);
            throw new InvalidSaleTimesException(sale.startTime(), sale.endTime());
        }
        final var durationMinutes = ((float) (sale.endTime().toInstant().toEpochMilli() - sale.startTime().toInstant().toEpochMilli()))
            / 60000.0F;
        log.info("Sale duration: {} minutes", durationMinutes);
        if (durationMinutes < minSaleDuration) {
            log.error("Failed to create FlashSale due to duration too short: " + sale);
            throw new SaleDurationTooShortException(
                String.format("Sale duration of %.2f minutes is less than minimum required duration of %.2f minutes",
                    durationMinutes, minSaleDuration),
                durationMinutes,
                minSaleDuration);
        }

        try {
            // Save the flash sale.
            final FlashSale s = new FlashSale(null, sale.title(), sale.startTime(), sale.endTime(), sale.status(), List.of());
            log.debug("Saving FlashSale: " + s);
            final var saved = sales.save(s);
            log.debug("Saved FlashSale result: " + saved);

            // For each product, create a flash sale item and update the reserved stock if
            // there is enough.
            final List<String> missingProducts = new ArrayList<>();
            final List<Product> notEnoughStockProducts = new ArrayList<>();
            final List<SaleProductDto> productList = sale.products() != null ? sale.products() : List.of();

            productList.forEach(sp -> {
                final var maybeProduct = products.findById(UUID.fromString(sp.id()));
                maybeProduct.ifPresentOrElse(p -> {
                    if (p.getReservedCount() + sp.reservedCount() > p.getTotalPhysicalStock()) {
                        notEnoughStockProducts.add(p);
                    } else {
                        final var item = new FlashSaleItem(null, saved, p, sp.reservedCount(), 0, p.getBasePrice());
                        items.save(item);
                        final int allocatedStock = sp.reservedCount() + p.getReservedCount();
                        p.setReservedCount(allocatedStock);
                        products.save(p);
                    }
                }, () -> {
                    missingProducts.add(sp.id());
                });
            });
            if (!missingProducts.isEmpty()) {
                final String ids = missingProducts.stream().collect(Collectors.joining(", "));
                log.error("Failed to create Flash Sale due to missing products: {}", ids);
                throw new ProductNotFoundException(ids);
            }
            if (!notEnoughStockProducts.isEmpty()) {
                final String ids = notEnoughStockProducts.stream().map(Product::getId).map(Object::toString).collect(Collectors.joining(", "));
                log.error("Failed to create Flash Sale due to not enough stock: {}", ids);
                throw new InsufficientResourcesException(
                    ids);
            }
            log.info("Created Flash Sale: " + saved);
            return saved.getId();
        } catch (final DuplicateKeyException e) {
            log.error("Unique constraint violated - duplicate key: {}", e.getMessage());
            throw new DuplicateEntityException(e.getMessage(), sale.title());
        } catch (final IllegalArgumentException e) {
            log.error("Invalid UUID String: {}", e.getMessage());
            throw new ProductNotFoundException(e.getMessage());
        }
    }

    /**
     * Activates DRAFT sales whose start time has passed.
     * This method is called by the scheduled job to transition sales from DRAFT to
     * ACTIVE.
     *
     * @return the number of sales activated
     */
    @Transactional
    @CacheEvict(value = {"draftSales", "activeSales"}, allEntries = true)
    public int activateDraftSales() {
        final OffsetDateTime now = OffsetDateTime.now();
        log.debug("Checking for DRAFT sales ready to activate at {}", now);

        final List<FlashSale> draftSales = sales.findDraftSalesReadyToActivate(SaleStatus.DRAFT, now);

        if (draftSales.isEmpty()) {
            log.debug("No DRAFT sales found ready to activate");
            return 0;
        }

        log.info("Found {} DRAFT sale(s) ready to activate", draftSales.size());

        int activatedCount = 0;
        for (final FlashSale sale : draftSales) {
            if (sale.getStatus() == SaleStatus.DRAFT && (sale.getStartTime().isBefore(now) || sale.getStartTime().isEqual(now))) {
                sale.setStatus(SaleStatus.ACTIVE);
                sales.save(sale);
                activatedCount++;
                log.info("Activated FlashSale: {} (startTime: {})", sale.getId(), sale.getStartTime());
            }
        }

        log.info("Activated {} DRAFT sale(s)", activatedCount);
        return activatedCount;
    }

    /**
     * Completes ACTIVE sales whose end time has passed.
     * This method is called by the scheduled job to transition sales from ACTIVE to
     * COMPLETED.
     *
     * @return the number of sales completed
     */
    @Transactional
    @CacheEvict(value = "activeSales", allEntries = true)
    public int completeActiveSales() {
        final OffsetDateTime now = OffsetDateTime.now();
        log.debug("Checking for ACTIVE sales ready to complete at {}", now);

        final List<FlashSale> activeSales = sales.findActiveSalesReadyToComplete(SaleStatus.ACTIVE, now);

        if (activeSales.isEmpty()) {
            log.debug("No ACTIVE sales found ready to complete");
            return 0;
        }

        log.info("Found {} ACTIVE sale(s) ready to complete", activeSales.size());

        int completedCount = 0;
        for (final FlashSale sale : activeSales) {
            if (sale.getStatus() == SaleStatus.ACTIVE && (sale.getEndTime().isBefore(now) || sale.getEndTime().isEqual(now))) {
                sale.setStatus(SaleStatus.COMPLETED);
                sales.save(sale);
                completedCount++;
                log.info("Completed FlashSale: {} (endTime: {})", sale.getId(), sale.getEndTime());

                // Release unsold stock for each sale item
                for (final FlashSaleItem item : sale.getItems()) {
                    final int difference = item.getAllocatedStock() - item.getSoldCount();
                    if (difference > 0) {
                        // Reduce allocated stock to match sold count
                        item.setAllocatedStock(item.getSoldCount());
                        items.save(item);

                        // Release reserved count from product
                        final Product product = item.getProduct();
                        final int newReservedCount = product.getReservedCount() - difference;
                        product.setReservedCount(newReservedCount);
                        products.save(product);

                        log.debug("Released {} unsold units for product {} in sale {}",
                            difference, product.getId(), sale.getId());
                    }
                }
            }
        }

        log.info("Completed {} ACTIVE sale(s)", completedCount);
        return completedCount;
    }

    /**
     * Cancels a flash sale by ID.
     * Only DRAFT and ACTIVE sales can be cancelled.
     * Releases reserved stock back to products.
     *
     * @param saleId
     *            the flash sale ID to cancel
     * @throws FlashSaleNotFoundException
     *             if the sale is not found
     * @throws IllegalArgumentException
     *             if the sale is already COMPLETED or CANCELLED
     */
    @Transactional
    @CacheEvict(value = {"flashSales", "activeSales"}, key = "#saleId")
    public void cancelFlashSale(final UUID saleId) {
        log.info("Cancelling FlashSale: {}", saleId);

        final FlashSale sale = sales.findById(saleId)
            .orElseThrow(() -> {
                log.error("Flash sale not found: {}", saleId);
                return new FlashSaleNotFoundException(saleId);
            });

        // Validate status: Only DRAFT or ACTIVE can be cancelled
        final SaleStatus previousStatus = sale.getStatus();
        if (previousStatus == SaleStatus.COMPLETED) {
            log.error("Cannot cancel COMPLETED sale: {}", saleId);
            throw new IllegalArgumentException("Cannot cancel a COMPLETED sale");
        }
        if (previousStatus == SaleStatus.CANCELLED) {
            log.error("Sale is already CANCELLED: {}", saleId);
            throw new IllegalArgumentException("Sale is already CANCELLED");
        }

        // Release stock for each sale item
        for (final FlashSaleItem item : sale.getItems()) {
            final int difference = item.getAllocatedStock() - item.getSoldCount();
            if (difference > 0) {
                // Reduce allocated stock to match sold count (0 for DRAFT, soldCount for
                // ACTIVE)
                item.setAllocatedStock(item.getSoldCount());
                items.save(item);

                // Release reserved count from product
                final Product product = item.getProduct();
                final int newReservedCount = product.getReservedCount() - difference;
                product.setReservedCount(newReservedCount);
                products.save(product);

                log.debug("Released {} units for product {} in sale {}",
                    difference, product.getId(), sale.getId());
            }
        }

        // Set sale status to CANCELLED
        sale.setStatus(SaleStatus.CANCELLED);
        sales.save(sale);
        log.info("Cancelled FlashSale: {} (previous status: {})", saleId, previousStatus);
    }

    /**
     * Get all flash sales with optional filters.
     *
     * @param status
     *            optional status filter
     * @param startDate
     *            optional filter window start (inclusive). When provided, only sales whose time period overlaps
     *            the specified window are returned.
     * @param endDate
     *            optional filter window end (inclusive). When provided, only sales whose time period overlaps
     *            the specified window are returned.
     * @return list of flash sales matching the filters
     */
    @Cacheable(value = "flashSales", key = "(#status != null ? #status.toString() : 'null') + ':' + (#startDate != null ? #startDate.toString() : 'null') + ':' + (#endDate != null ? #endDate.toString() : 'null')")
    public List<FlashSaleResponseDto> getAllFlashSales(final SaleStatus status, final OffsetDateTime startDate, final OffsetDateTime endDate) {
        log.debug("Getting all flash sales with filters: status={}, startDate={}, endDate={}", status, startDate, endDate);
        final List<FlashSale> flashSales = sales.findAllWithFilters(status, startDate, endDate);
        final List<FlashSaleResponseDto> result = flashSales.stream()
            .map(this::mapToResponseDto)
            .collect(Collectors.toList());
        log.info("Found {} flash sale(s)", result.size());
        return result;
    }

    /**
     * Get a flash sale by ID.
     *
     * @param id
     *            the flash sale ID
     * @return the flash sale DTO
     * @throws FlashSaleNotFoundException
     *             if the sale is not found
     */
    @Cacheable(value = "flashSales", key = "#id")
    public FlashSaleResponseDto getFlashSaleById(final UUID id) {
        log.debug("Getting flash sale by ID: {}", id);
        final FlashSale sale = sales.findByIdWithItems(id)
            .orElseThrow(() -> {
                log.error("Flash sale not found: {}", id);
                return new FlashSaleNotFoundException(id);
            });
        return mapToResponseDto(sale);
    }

    /**
     * Update a flash sale.
     *
     * @param id
     *            the flash sale ID
     * @param updateDto
     *            the update DTO
     * @return the updated flash sale DTO
     * @throws FlashSaleNotFoundException
     *             if the sale is not found
     * @throws InvalidSaleTimesException
     *             if the times are invalid
     * @throws SaleDurationTooShortException
     *             if the duration is too short
     */
    @Transactional
    @CacheEvict(value = {"flashSales", "activeSales", "draftSales"}, key = "#id", allEntries = true)
    public FlashSaleResponseDto updateFlashSale(final UUID id, @Valid final UpdateFlashSaleDto updateDto) {
        log.info("Updating FlashSale: {} with {}", id, updateDto);

        final FlashSale sale = sales.findById(id)
            .orElseThrow(() -> {
                log.error("Flash sale not found: {}", id);
                return new FlashSaleNotFoundException(id);
            });

        // Determine the times to use (updateDto values or existing values)
        final OffsetDateTime newStartTime = updateDto.startTime() != null ? updateDto.startTime() : sale.getStartTime();
        final OffsetDateTime newEndTime = updateDto.endTime() != null ? updateDto.endTime() : sale.getEndTime();

        // Validate times if both are provided or being updated
        if (updateDto.startTime() != null || updateDto.endTime() != null) {
            if (!newStartTime.isBefore(newEndTime)) {
                log.error("Failed to update FlashSale due to start-after-end: startTime={}, endTime={}", newStartTime, newEndTime);
                throw new InvalidSaleTimesException(newStartTime, newEndTime);
            }

            // Validate minimum duration
            final var durationMinutes = ((float) (newEndTime.toInstant().toEpochMilli() - newStartTime.toInstant().toEpochMilli()))
                / 60000.0F;
            log.debug("Sale duration: {} minutes", durationMinutes);
            if (durationMinutes < minSaleDuration) {
                log.error("Failed to update FlashSale due to duration too short: duration={}, min={}", durationMinutes, minSaleDuration);
                throw new SaleDurationTooShortException(
                    String.format("Sale duration of %.2f minutes is less than minimum required duration of %.2f minutes",
                        durationMinutes, minSaleDuration),
                    durationMinutes,
                    minSaleDuration);
            }
        }

        // Update fields
        if (updateDto.title() != null && !updateDto.title().trim().isEmpty()) {
            sale.setTitle(updateDto.title());
        }
        if (updateDto.startTime() != null) {
            sale.setStartTime(updateDto.startTime());
        }
        if (updateDto.endTime() != null) {
            sale.setEndTime(updateDto.endTime());
        }

        final FlashSale saved = sales.save(sale);
        log.info("Updated FlashSale: {}", saved.getId());

        // Reload with items for response
        return mapToResponseDto(sales.findByIdWithItems(saved.getId()).orElse(saved));
    }

    /**
     * Delete a flash sale (only DRAFT status allowed).
     * Releases reserved stock back to products.
     *
     * @param id
     *            the flash sale ID
     * @throws FlashSaleNotFoundException
     *             if the sale is not found
     * @throws IllegalArgumentException
     *             if the sale is not in DRAFT status
     */
    @Transactional
    @CacheEvict(value = {"flashSales", "draftSales"}, key = "#id", allEntries = true)
    public void deleteFlashSale(final UUID id) {
        log.info("Deleting FlashSale: {}", id);

        final FlashSale sale = sales.findByIdWithItems(id)
            .orElseThrow(() -> {
                log.error("Flash sale not found: {}", id);
                return new FlashSaleNotFoundException(id);
            });

        // Validate status: Only DRAFT can be deleted
        if (sale.getStatus() != SaleStatus.DRAFT) {
            log.error("Cannot delete flash sale with status {}: {}", sale.getStatus(), id);
            throw new IllegalArgumentException("Only DRAFT flash sales can be deleted");
        }

        // Release reserved stock for each sale item
        for (final FlashSaleItem item : sale.getItems()) {
            final int difference = item.getAllocatedStock() - item.getSoldCount();
            if (difference > 0) {
                // Release reserved count from product
                final Product product = item.getProduct();
                final int newReservedCount = product.getReservedCount() - difference;
                product.setReservedCount(newReservedCount);
                products.save(product);

                log.debug("Released {} units for product {} in sale {}",
                    difference, product.getId(), sale.getId());
            }
        }

        // Delete the flash sale (cascade will handle items)
        sales.delete(sale);
        log.info("Deleted FlashSale: {}", id);
    }

    /**
     * Add items to an existing flash sale.
     * Only DRAFT sales can have items added.
     *
     * @param saleId
     *            the flash sale ID
     * @param items
     *            the items to add
     * @return the updated flash sale DTO
     * @throws FlashSaleNotFoundException
     *             if the sale is not found
     * @throws IllegalArgumentException
     *             if the sale is not in DRAFT status
     * @throws ProductNotFoundException
     *             if any product is not found
     * @throws InsufficientResourcesException
     *             if there is not enough stock
     * @throws IllegalArgumentException
     *             if a product is already in the sale
     */
    @Transactional
    @CacheEvict(value = {"flashSales", "draftSales"}, key = "#saleId", allEntries = true)
    public FlashSaleResponseDto addItemsToFlashSale(final UUID saleId, @Valid final List<AddFlashSaleItemDto> items) {
        log.info("Adding items to FlashSale: {}", saleId);

        final FlashSale sale = sales.findByIdWithItems(saleId)
            .orElseThrow(() -> {
                log.error("Flash sale not found: {}", saleId);
                return new FlashSaleNotFoundException(saleId);
            });

        // Validate status: Only DRAFT can have items added
        if (sale.getStatus() != SaleStatus.DRAFT) {
            log.error("Cannot add items to flash sale with status {}: {}", sale.getStatus(), saleId);
            throw new IllegalArgumentException("Only DRAFT flash sales can have items added");
        }

        final List<String> missingProducts = new ArrayList<>();
        final List<Product> notEnoughStockProducts = new ArrayList<>();
        final List<String> duplicateProducts = new ArrayList<>();

        for (final AddFlashSaleItemDto itemDto : items) {
            final var maybeProduct = products.findById(UUID.fromString(itemDto.productId()));
            maybeProduct.ifPresentOrElse(p -> {
                // Check if product is already in the sale
                if (this.items.existsByFlashSaleIdAndProductId(saleId, p.getId())) {
                    duplicateProducts.add(itemDto.productId());
                    return;
                }

                // Validate stock availability
                if (p.getReservedCount() + itemDto.allocatedStock() > p.getTotalPhysicalStock()) {
                    notEnoughStockProducts.add(p);
                    return;
                }

                // Create flash sale item
                final BigDecimal salePrice = itemDto.salePrice() != null ? itemDto.salePrice() : p.getBasePrice();
                final var flashSaleItem = new FlashSaleItem(null, sale, p, itemDto.allocatedStock(), 0, salePrice);
                this.items.save(flashSaleItem);

                // Update product reserved count
                final int newReservedCount = p.getReservedCount() + itemDto.allocatedStock();
                p.setReservedCount(newReservedCount);
                products.save(p);

                log.debug("Added item for product {} to sale {}: allocatedStock={}, salePrice={}",
                    p.getId(), saleId, itemDto.allocatedStock(), salePrice);
            }, () -> {
                missingProducts.add(itemDto.productId());
            });
        }

        if (!missingProducts.isEmpty()) {
            final String ids = missingProducts.stream().collect(Collectors.joining(", "));
            log.error("Failed to add items to Flash Sale due to missing products: {}", ids);
            throw new ProductNotFoundException(ids);
        }

        if (!duplicateProducts.isEmpty()) {
            final String ids = duplicateProducts.stream().collect(Collectors.joining(", "));
            log.error("Failed to add items to Flash Sale due to duplicate products: {}", ids);
            throw new IllegalArgumentException("Products already in sale: " + ids);
        }

        if (!notEnoughStockProducts.isEmpty()) {
            final String ids = notEnoughStockProducts.stream().map(Product::getId).map(Object::toString).collect(Collectors.joining(", "));
            log.error("Failed to add items to Flash Sale due to not enough stock: {}", ids);
            throw new InsufficientResourcesException(ids);
        }

        log.info("Added {} item(s) to FlashSale: {}", items.size(), saleId);

        // Reload with items for response
        return mapToResponseDto(sales.findByIdWithItems(saleId).orElse(sale));
    }

    /**
     * Update a flash sale item.
     * Only DRAFT sales can have items updated.
     *
     * @param saleId
     *            the flash sale ID
     * @param itemId
     *            the flash sale item ID
     * @param updateDto
     *            the update DTO
     * @return the updated flash sale DTO
     * @throws FlashSaleNotFoundException
     *             if the sale is not found
     * @throws FlashSaleItemNotFoundException
     *             if the item is not found
     * @throws IllegalArgumentException
     *             if the sale is not in DRAFT status or validation fails
     * @throws InsufficientResourcesException
     *             if there is not enough stock
     */
    @Transactional
    @CacheEvict(value = {"flashSales", "draftSales"}, key = "#saleId", allEntries = true)
    public FlashSaleResponseDto updateFlashSaleItem(final UUID saleId, final UUID itemId, @Valid final UpdateFlashSaleItemDto updateDto) {
        log.info("Updating FlashSaleItem: {} in sale {}", itemId, saleId);

        final FlashSale sale = sales.findByIdWithItems(saleId)
            .orElseThrow(() -> {
                log.error("Flash sale not found: {}", saleId);
                return new FlashSaleNotFoundException(saleId);
            });

        // Validate status: Only DRAFT can have items updated
        if (sale.getStatus() != SaleStatus.DRAFT) {
            log.error("Cannot update items in flash sale with status {}: {}", sale.getStatus(), saleId);
            throw new IllegalArgumentException("Only DRAFT flash sales can have items updated");
        }

        final FlashSaleItem item = items.findByIdAndFlashSaleId(itemId, saleId)
            .orElseThrow(() -> {
                log.error("Flash sale item not found: {} in sale {}", itemId, saleId);
                return new FlashSaleItemNotFoundException(itemId);
            });

        final Product product = item.getProduct();
        boolean updated = false;

        // Update allocatedStock if provided
        if (updateDto.allocatedStock() != null) {
            final int newAllocatedStock = updateDto.allocatedStock();
            final int oldAllocatedStock = item.getAllocatedStock();

            // Validate: new allocatedStock must be >= soldCount
            if (newAllocatedStock < item.getSoldCount()) {
                log.error("Cannot reduce allocatedStock below soldCount: allocatedStock={}, soldCount={}",
                    newAllocatedStock, item.getSoldCount());
                throw new IllegalArgumentException(
                    String.format("Allocated stock (%d) cannot be less than sold count (%d)",
                        newAllocatedStock, item.getSoldCount()));
            }

            if (newAllocatedStock != oldAllocatedStock) {
                final int difference = newAllocatedStock - oldAllocatedStock;

                // Validate product has enough stock
                if (product.getReservedCount() + difference > product.getTotalPhysicalStock()) {
                    log.error("Insufficient stock to update item: currentReserved={}, difference={}, totalStock={}",
                        product.getReservedCount(), difference, product.getTotalPhysicalStock());
                    throw new InsufficientResourcesException(product.getId().toString());
                }

                item.setAllocatedStock(newAllocatedStock);
                product.setReservedCount(product.getReservedCount() + difference);
                products.save(product);
                updated = true;

                log.debug("Updated allocatedStock for item {}: {} -> {}", itemId, oldAllocatedStock, newAllocatedStock);
            }
        }

        // Update salePrice if provided
        if (updateDto.salePrice() != null && !updateDto.salePrice().equals(item.getSalePrice())) {
            item.setSalePrice(updateDto.salePrice());
            updated = true;
            log.debug("Updated salePrice for item {}: {}", itemId, updateDto.salePrice());
        }

        if (updated) {
            items.save(item);
            log.info("Updated FlashSaleItem: {} in sale {}", itemId, saleId);
        }

        // Reload with items for response
        return mapToResponseDto(sales.findByIdWithItems(saleId).orElse(sale));
    }

    /**
     * Remove a flash sale item from a sale.
     * Only DRAFT sales can have items removed.
     *
     * @param saleId
     *            the flash sale ID
     * @param itemId
     *            the flash sale item ID
     * @return the updated flash sale DTO
     * @throws FlashSaleNotFoundException
     *             if the sale is not found
     * @throws FlashSaleItemNotFoundException
     *             if the item is not found
     * @throws IllegalArgumentException
     *             if the sale is not in DRAFT status
     */
    @Transactional
    @CacheEvict(value = {"flashSales", "draftSales"}, key = "#saleId", allEntries = true)
    public FlashSaleResponseDto removeFlashSaleItem(final UUID saleId, final UUID itemId) {
        log.info("Removing FlashSaleItem: {} from sale {}", itemId, saleId);

        final FlashSale sale = sales.findByIdWithItems(saleId)
            .orElseThrow(() -> {
                log.error("Flash sale not found: {}", saleId);
                return new FlashSaleNotFoundException(saleId);
            });

        // Validate status: Only DRAFT can have items removed
        if (sale.getStatus() != SaleStatus.DRAFT) {
            log.error("Cannot remove items from flash sale with status {}: {}", sale.getStatus(), saleId);
            throw new IllegalArgumentException("Only DRAFT flash sales can have items removed");
        }

        final FlashSaleItem item = items.findByIdAndFlashSaleId(itemId, saleId)
            .orElseThrow(() -> {
                log.error("Flash sale item not found: {} in sale {}", itemId, saleId);
                return new FlashSaleItemNotFoundException(itemId);
            });

        final Product product = item.getProduct();

        // Calculate stock to release (allocatedStock - soldCount)
        final int stockToRelease = item.getAllocatedStock() - item.getSoldCount();

        if (stockToRelease > 0) {
            // Update product reserved count
            final int newReservedCount = product.getReservedCount() - stockToRelease;
            product.setReservedCount(newReservedCount);
            products.save(product);

            log.debug("Released {} units for product {} in sale {}",
                stockToRelease, product.getId(), saleId);
        }

        // Delete the item
        items.delete(item);
        log.info("Removed FlashSaleItem: {} from sale {}", itemId, saleId);

        // Reload with items for response
        return mapToResponseDto(sales.findByIdWithItems(saleId).orElse(sale));
    }

    /**
     * Map a FlashSale entity to FlashSaleResponseDto.
     *
     * @param sale
     *            the flash sale entity
     * @return the response DTO
     */
    private FlashSaleResponseDto mapToResponseDto(final FlashSale sale) {
        final List<FlashSaleItemDto> itemDtos = sale.getItems().stream()
            .map(item -> new FlashSaleItemDto(
                item.getId().toString(),
                item.getProduct().getId().toString(),
                item.getProduct().getName(),
                item.getAllocatedStock(),
                item.getSoldCount(),
                item.getSalePrice()))
            .collect(Collectors.toList());

        return new FlashSaleResponseDto(
            sale.getId().toString(),
            sale.getTitle(),
            sale.getStartTime(),
            sale.getEndTime(),
            sale.getStatus(),
            itemDtos);
    }

}
