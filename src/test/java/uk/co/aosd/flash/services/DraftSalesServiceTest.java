package uk.co.aosd.flash.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import uk.co.aosd.flash.domain.FlashSale;
import uk.co.aosd.flash.domain.FlashSaleItem;
import uk.co.aosd.flash.domain.Product;
import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.ClientDraftSaleDto;
import uk.co.aosd.flash.repository.FlashSaleRepository;

/**
 * Test the Draft Sales Service.
 */
public class DraftSalesServiceTest {

    private static final String saleId1 = "547cf74d-7b64-44ea-b70f-cbcde09cadc9";
    private static final String saleId2 = "1c05690e-cd9a-42ee-9f15-194b4c454216";
    private static final String productId1 = "11111111-1111-1111-1111-111111111111";
    private static final String productId2 = "22222222-2222-2222-2222-222222222222";

    private FlashSaleRepository repository = Mockito.mock(FlashSaleRepository.class);

    private DraftSalesService service = new DraftSalesService(repository);

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(repository);
    }

    @Test
    public void shouldFindDraftSalesEmpty() {
        Mockito.when(repository.findDraftSalesWithinDays(
            ArgumentMatchers.eq(SaleStatus.DRAFT),
            ArgumentMatchers.any(OffsetDateTime.class),
            ArgumentMatchers.any(OffsetDateTime.class))).thenReturn(List.of());
        final var draftSales = service.getDraftSalesWithinDays(7);
        assertNotNull(draftSales);
        assertTrue(draftSales.isEmpty());
    }

    @Test
    public void shouldFindTwoDraftSales() {
        final var now = OffsetDateTime.now().withSecond(0).withNano(0);
        final var startTime1 = now.plusDays(1);
        final var endTime1 = startTime1.plusHours(1);
        final var startTime2 = now.plusDays(3);
        final var endTime2 = startTime2.plusHours(1);

        final var product1 = new Product(UUID.fromString(productId1), "Product 1", "Description 1", 100, BigDecimal.valueOf(99.99), 10);
        final var product2 = new Product(UUID.fromString(productId2), "Product 2", "Description 2", 200, BigDecimal.valueOf(199.99), 20);

        final var sale1 = new FlashSale(UUID.fromString(saleId1), "Draft Sale 1", startTime1, endTime1, SaleStatus.DRAFT, new ArrayList<>());
        final var sale2 = new FlashSale(UUID.fromString(saleId2), "Draft Sale 2", startTime2, endTime2, SaleStatus.DRAFT, new ArrayList<>());

        final var item1 = new FlashSaleItem(null, sale1, product1, 10, 0, BigDecimal.valueOf(89.99));
        final var item2 = new FlashSaleItem(null, sale2, product2, 20, 0, BigDecimal.valueOf(179.99));

        sale1.getItems().add(item1);
        sale2.getItems().add(item2);

        final List<FlashSale> seed = List.of(sale1, sale2);
        Mockito.when(repository.findDraftSalesWithinDays(
            ArgumentMatchers.eq(SaleStatus.DRAFT),
            ArgumentMatchers.any(OffsetDateTime.class),
            ArgumentMatchers.any(OffsetDateTime.class))).thenReturn(seed);

        final var draftSales = service.getDraftSalesWithinDays(7);

        assertNotNull(draftSales);
        assertEquals(2, draftSales.size());
        assertEquals(1, draftSales.stream().filter(s -> s.saleId().equals(saleId1)).count());
        assertEquals(1, draftSales.stream().filter(s -> s.saleId().equals(saleId2)).count());
    }

    @Test
    public void shouldMapFlashSaleToDtoCorrectly() {
        final var now = OffsetDateTime.now().withSecond(0).withNano(0);
        final var startTime = now.plusDays(1);
        final var endTime = startTime.plusHours(1);

        final var product1 = new Product(UUID.fromString(productId1), "Product 1", "Description 1", 100, BigDecimal.valueOf(99.99), 10);
        final var product2 = new Product(UUID.fromString(productId2), "Product 2", "Description 2", 200, BigDecimal.valueOf(199.99), 20);

        final var sale = new FlashSale(UUID.fromString(saleId1), "Test Draft Sale", startTime, endTime, SaleStatus.DRAFT, new ArrayList<>());

        final var item1 = new FlashSaleItem(null, sale, product1, 15, 0, BigDecimal.valueOf(89.99));
        final var item2 = new FlashSaleItem(null, sale, product2, 25, 0, BigDecimal.valueOf(179.99));

        sale.getItems().add(item1);
        sale.getItems().add(item2);

        Mockito.when(repository.findDraftSalesWithinDays(
            ArgumentMatchers.eq(SaleStatus.DRAFT),
            ArgumentMatchers.any(OffsetDateTime.class),
            ArgumentMatchers.any(OffsetDateTime.class))).thenReturn(List.of(sale));

        final var draftSales = service.getDraftSalesWithinDays(7);

        assertNotNull(draftSales);
        assertEquals(1, draftSales.size());

        final ClientDraftSaleDto dto = draftSales.get(0);
        assertEquals(saleId1, dto.saleId());
        assertEquals("Test Draft Sale", dto.title());
        assertEquals(startTime, dto.startTime());
        assertEquals(endTime, dto.endTime());
        assertEquals(2, dto.products().size());
        assertEquals(productId1, dto.products().get(0).productId());
        assertEquals(15, dto.products().get(0).allocatedStock());
        assertEquals(0, BigDecimal.valueOf(89.99).compareTo(dto.products().get(0).salePrice()));
        assertEquals(productId2, dto.products().get(1).productId());
        assertEquals(25, dto.products().get(1).allocatedStock());
        assertEquals(0, BigDecimal.valueOf(179.99).compareTo(dto.products().get(1).salePrice()));
    }
}
