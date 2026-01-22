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

    @Test
    public void shouldFindDraftSalesWithinDays() {
        final var now = OffsetDateTime.now();
        final var currentTime = now.withSecond(0).withNano(0);
        final var futureTime = currentTime.plusDays(7);

        // Create draft sales within the next 7 days
        final var draftSale1 = new FlashSale(null, "Draft Sale 1", currentTime.plusDays(1), currentTime.plusDays(1).plusHours(1), SaleStatus.DRAFT, List.of());
        final var draftSale2 = new FlashSale(null, "Draft Sale 2", currentTime.plusDays(3), currentTime.plusDays(3).plusHours(1), SaleStatus.DRAFT, List.of());
        final var draftSale3 = new FlashSale(null, "Draft Sale 3", currentTime.plusDays(5), currentTime.plusDays(5).plusHours(1), SaleStatus.DRAFT, List.of());

        // Create a draft sale outside the range (more than 7 days)
        final var draftSale4 = new FlashSale(null, "Draft Sale 4", currentTime.plusDays(10), currentTime.plusDays(10).plusHours(1), SaleStatus.DRAFT, List.of());

        // Create a draft sale in the past
        final var draftSale5 = new FlashSale(null, "Draft Sale 5", currentTime.minusDays(1), currentTime.minusDays(1).plusHours(1), SaleStatus.DRAFT, List.of());

        // Create an active sale (should not be included)
        final var activeSale = new FlashSale(null, "Active Sale", currentTime.plusDays(2), currentTime.plusDays(2).plusHours(1), SaleStatus.ACTIVE, List.of());

        sales.save(draftSale1);
        sales.save(draftSale2);
        sales.save(draftSale3);
        sales.save(draftSale4);
        sales.save(draftSale5);
        sales.save(activeSale);

        final var draftSales = sales.findDraftSalesWithinDays(SaleStatus.DRAFT, currentTime, futureTime);
        assertEquals(3, draftSales.size());
        assertEquals("Draft Sale 1", draftSales.get(0).getTitle());
        assertEquals("Draft Sale 2", draftSales.get(1).getTitle());
        assertEquals("Draft Sale 3", draftSales.get(2).getTitle());
    }

}
