package uk.co.aosd.flash.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;
import uk.co.aosd.flash.domain.FlashSale;
import uk.co.aosd.flash.domain.FlashSaleItem;
import uk.co.aosd.flash.domain.Product;
import uk.co.aosd.flash.domain.SaleStatus;

/**
 * Test the RemainingActiveStock Repository.
 */
@DataJpaTest
public class RemainingActiveStockRepositoryTest {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:latest");

    @Autowired
    private RemainingActiveStockRepository remainingActiveStockRepository;

    @Autowired
    private FlashSaleRepository sales;

    @Autowired
    private ProductRepository products;

    @Autowired
    private FlashSaleItemRepository items;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void shouldFindRemainingActiveStock() {
        // Create an active sale with remaining stock
        final var startTime = OffsetDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        final var endTime = startTime.plusHours(1);
        final var flashSale = new FlashSale(null, "Active Sale", startTime, endTime, SaleStatus.ACTIVE, List.of());
        final var savedFlashSale = sales.save(flashSale);

        final var product = new Product(null, "Product 1", "Product 1 description", 100, BigDecimal.valueOf(99.99), 0);
        final var savedProduct = products.save(product);

        final var saleItem = new FlashSaleItem(null, savedFlashSale, savedProduct, 10, 5, BigDecimal.valueOf(89.99));
        final var savedSaleItem = items.save(saleItem);

        // Create an inactive sale
        final var inactiveSale = new FlashSale(null, "Inactive Sale", startTime, endTime, SaleStatus.COMPLETED, List.of());
        sales.save(inactiveSale);
        items.save(new FlashSaleItem(null, inactiveSale, savedProduct, 10, 0, BigDecimal.valueOf(89.99)));

        // Create a sold out sale item
        final var soldOutSale = new FlashSale(null, "Sold Out Sale", startTime, endTime, SaleStatus.ACTIVE, List.of());
        sales.save(soldOutSale);
        items.save(new FlashSaleItem(null, soldOutSale, savedProduct, 10, 10, BigDecimal.valueOf(89.99)));

        // Flush to ensure data is in DB before querying the view
        entityManager.flush();

        // Verify that only the active sale with remaining stock is visible in the view
        final var results = remainingActiveStockRepository.findAll();
        assertEquals(1, results.size());
        
        final var result = results.get(0);
        assertEquals(savedSaleItem.getId(), result.getItemId());
        assertEquals(savedFlashSale.getId(), result.getSaleId());
        assertEquals("Active Sale", result.getTitle());
        assertEquals(savedProduct.getId(), result.getProductId());
        assertEquals(10, result.getAllocatedStock());
        assertEquals(5, result.getSoldCount());
        assertEquals(0, BigDecimal.valueOf(89.99).compareTo(result.getSalePrice()));
    }

    @Test
    public void shouldFindById() {
        final var startTime = OffsetDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        final var endTime = startTime.plusHours(1);
        final var flashSale = new FlashSale(null, "Active Sale", startTime, endTime, SaleStatus.ACTIVE, List.of());
        final var savedFlashSale = sales.save(flashSale);

        final var product = new Product(null, "Product 1", "Product 1 description", 100, BigDecimal.valueOf(99.99), 0);
        final var savedProduct = products.save(product);

        final var saleItem = new FlashSaleItem(null, savedFlashSale, savedProduct, 10, 5, BigDecimal.valueOf(89.99));
        final var savedSaleItem = items.save(saleItem);

        entityManager.flush();

        final var resultOpt = remainingActiveStockRepository.findById(savedSaleItem.getId());
        assertTrue(resultOpt.isPresent());
        assertEquals(savedSaleItem.getId(), resultOpt.get().getItemId());
    }
}
