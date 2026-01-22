package uk.co.aosd.flash.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

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
 * Test the Flash Sale Item Repository.
 */
@DataJpaTest
public class FlashSaleItemRepositoryTest {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres");

    @Autowired
    private FlashSaleItemRepository items;

    @Autowired
    private ProductRepository products;

    @Autowired
    private FlashSaleRepository sales;

    @Test
    public void shouldIncrementSoldCount() {
        UUID saleItemUuid = null;
        {
            //
            // Create a Flash Sale
            //
            final var startTime = OffsetDateTime.of(2026, 01, 01, 12, 0, 0, 0, ZoneOffset.UTC);
            final var endTime = startTime.plusHours(1);
            final var flashSale = new FlashSale(null, "Test Sale 1", startTime, endTime, SaleStatus.ACTIVE, List.of());
            final var savedFlashSale = sales.save(flashSale);

            //
            // Create a product.
            //
            final var product = new Product(null, "Product 1", "Product 1 description", 100, BigDecimal.valueOf(99.99), 10);
            final var savedProduct = products.save(product);

            //
            // Create a Sale Item
            //
            final var saleItem = new FlashSaleItem(null, savedFlashSale, savedProduct, 10, 1, BigDecimal.valueOf(99.99));
            final var savedSaleItem = items.save(saleItem);
            saleItemUuid = savedSaleItem.getId();
            assertEquals(1, savedSaleItem.getSoldCount());
        }
        {
            // Verify the sold count
            final var updatedCount = items.incrementSoldCount(saleItemUuid, 1);
            assertEquals(1, updatedCount, "Should have updated exactly 1 row.");

            final var updatedItem = items.findById(saleItemUuid);
            assertTrue(updatedItem.isPresent());
            updatedItem.ifPresent(i -> {
                assertEquals(2, i.getSoldCount());
            });
        }
    }

    @Test
    public void shouldFailtToIncrementSoldCountDueToInactiveSale() {
        UUID saleItemUuid = null;
        {
            //
            // Create a Flash Sale
            //
            final var startTime = OffsetDateTime.of(2026, 01, 01, 12, 0, 0, 0, ZoneOffset.UTC);
            final var endTime = startTime.plusHours(1);
            final var flashSale = new FlashSale(null, "Test Sale 1", startTime, endTime, SaleStatus.COMPLETED, List.of());
            final var savedFlashSale = sales.save(flashSale);

            //
            // Create a product.
            //
            final var product = new Product(null, "Product 1", "Product 1 description", 100, BigDecimal.valueOf(99.99), 10);
            final var savedProduct = products.save(product);

            //
            // Create a Sale Item
            //
            final var saleItem = new FlashSaleItem(null, savedFlashSale, savedProduct, 10, 1, BigDecimal.valueOf(99.99));
            final var savedSaleItem = items.save(saleItem);
            saleItemUuid = savedSaleItem.getId();
            assertEquals(1, savedSaleItem.getSoldCount());
        }
        {
            // Verify the sold count
            final var updatedCount = items.incrementSoldCount(saleItemUuid, 1);
            assertEquals(0, updatedCount, "Should have updated exactly 0 rows.");

            final var updatedItem = items.findById(saleItemUuid);
            assertTrue(updatedItem.isPresent());
            updatedItem.ifPresent(i -> {
                assertEquals(1, i.getSoldCount());
            });
        }
    }

    @Test
    public void shouldFailToIncrementSoldCountDueToSoldOut() {
        UUID saleItemUuid = null;
        {
            //
            // Create a Flash Sale
            //
            final var startTime = OffsetDateTime.of(2026, 01, 01, 12, 0, 0, 0, ZoneOffset.UTC);
            final var endTime = startTime.plusHours(1);
            final var flashSale = new FlashSale(null, "Test Sale 1", startTime, endTime, SaleStatus.ACTIVE, List.of());
            final var savedFlashSale = sales.save(flashSale);

            //
            // Create a product.
            //
            final var product = new Product(null, "Product 1", "Product 1 description", 100, BigDecimal.valueOf(99.99), 10);
            final var savedProduct = products.save(product);

            //
            // Create a Sale Item
            //
            final var saleItem = new FlashSaleItem(null, savedFlashSale, savedProduct, 10, 10, BigDecimal.valueOf(99.99));
            final var savedSaleItem = items.save(saleItem);
            saleItemUuid = savedSaleItem.getId();
            assertEquals(10, savedSaleItem.getSoldCount());
        }
        {
            // Verify the sold count
            final var updatedCount = items.incrementSoldCount(saleItemUuid, 1);
            assertEquals(0, updatedCount, "Should have updated exactly 0 rows.");

            final var updatedItem = items.findById(saleItemUuid);
            assertTrue(updatedItem.isPresent());
            updatedItem.ifPresent(i -> {
                assertEquals(10, i.getSoldCount());
            });
        }
    }

}
