package uk.co.aosd.flash.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

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
public class FlashSaleRepositoryTest {

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
    public void shouldFindActiveSales() {
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
            assertEquals(1, savedSaleItem.getSoldCount());
        }
        {
            final var activeSales = sales.findByStatus(SaleStatus.ACTIVE);
            assertEquals(1, activeSales.size());
            final var completedSales = sales.findByStatus(SaleStatus.COMPLETED);
            assertEquals(0, completedSales.size());
        }
    }

}
