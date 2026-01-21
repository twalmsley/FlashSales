package uk.co.aosd.flash.services;

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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import uk.co.aosd.flash.domain.FlashSale;
import uk.co.aosd.flash.domain.FlashSaleItem;
import uk.co.aosd.flash.domain.Product;
import uk.co.aosd.flash.dto.CreateSaleDto;
import uk.co.aosd.flash.exc.*;
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
            throw new SaleDurationTooShortException("Sale duration of " + durationMinutes + " minutes is less than " + minSaleDuration);
        }

        try {
            // Save the flash sale.
            final FlashSale s = new FlashSale(null, sale.title(), sale.startTime(), sale.endTime(), sale.status());
            log.debug("Saving FlashSale: " + s);
            final var saved = sales.save(s);
            log.debug("Saved FlashSale result: " + saved);

            // For each product, create a flash sale item and update the reserved stock if
            // there is enough.
            final List<String> missingProducts = new ArrayList<>();
            final List<Product> notEnoughStockProducts = new ArrayList<>();

            sale.products().forEach(sp -> {
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

}
