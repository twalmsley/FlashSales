package uk.co.aosd.flash.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.co.aosd.flash.domain.RemainingActiveStock;
import uk.co.aosd.flash.dto.ClientActiveSaleDto;
import uk.co.aosd.flash.repository.RemainingActiveStockRepository;

/**
 * Test the Active Sales Service.
 */
public class ActiveSalesServiceTest {

    private static final String saleId1 = "547cf74d-7b64-44ea-b70f-cbcde09cadc9";
    private static final String saleId2 = "1c05690e-cd9a-42ee-9f15-194b4c454216";
    private static final String itemId1 = "ab3b715e-e2c2-4c28-925d-83ac93c32d02";
    private static final String itemId2 = "d4e5f6a7-b8c9-d0e1-f2a3-b4c5d6e7f8a9";
    private static final String productId1 = "11111111-1111-1111-1111-111111111111";
    private static final String productId2 = "22222222-2222-2222-2222-222222222222";

    private RemainingActiveStockRepository repository = Mockito.mock(RemainingActiveStockRepository.class);

    private ActiveSalesService service = new ActiveSalesService(repository);

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(repository);
    }

    @Test
    public void shouldFindAllActiveSalesEmpty() {
        Mockito.when(repository.findAll()).thenReturn(List.of());
        final var activeSales = service.getActiveSales();
        assertNotNull(activeSales);
        assertTrue(activeSales.isEmpty());
    }

    @Test
    public void shouldFindTwoActiveSales() {
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = startTime.plusHours(1);

        final RemainingActiveStock stock1 = new RemainingActiveStock(
            UUID.fromString(itemId1),
            UUID.fromString(saleId1),
            "Sale 1",
            startTime,
            endTime,
            UUID.fromString(productId1),
            10,
            5,
            BigDecimal.valueOf(89.99),
            "Product 1",
            "Description 1",
            BigDecimal.valueOf(99.99));

        final RemainingActiveStock stock2 = new RemainingActiveStock(
            UUID.fromString(itemId2),
            UUID.fromString(saleId2),
            "Sale 2",
            startTime.plusDays(1),
            endTime.plusDays(1),
            UUID.fromString(productId2),
            20,
            10,
            BigDecimal.valueOf(79.99),
            "Product 2",
            "Description 2",
            BigDecimal.valueOf(89.99));

        final List<RemainingActiveStock> seed = List.of(stock1, stock2);
        Mockito.when(repository.findAll()).thenReturn(seed);

        final var activeSales = service.getActiveSales();

        assertNotNull(activeSales);
        assertEquals(2, activeSales.size());
        assertEquals(1, activeSales.stream().filter(s -> s.saleId().equals(saleId1)).count());
        assertEquals(1, activeSales.stream().filter(s -> s.saleId().equals(saleId2)).count());
    }

    @Test
    public void shouldMapRemainingActiveStockToDtoCorrectly() {
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = startTime.plusHours(1);

        final RemainingActiveStock stock = new RemainingActiveStock(
            UUID.fromString(itemId1),
            UUID.fromString(saleId1),
            "Test Sale",
            startTime,
            endTime,
            UUID.fromString(productId1),
            15,
            8,
            BigDecimal.valueOf(99.99),
            "Test Product",
            "Test description",
            BigDecimal.valueOf(109.99));

        Mockito.when(repository.findAll()).thenReturn(List.of(stock));

        final var activeSales = service.getActiveSales();

        assertNotNull(activeSales);
        assertEquals(1, activeSales.size());

        final ClientActiveSaleDto dto = activeSales.get(0);
        assertEquals(saleId1, dto.saleId());
        assertEquals(itemId1, dto.flashSaleItemId());
        assertEquals("Test Sale", dto.title());
        assertEquals(startTime, dto.startTime());
        assertEquals(endTime, dto.endTime());
        assertEquals(productId1, dto.productId());
        assertEquals("Test Product", dto.productName());
        assertEquals("Test description", dto.productDescription());
        assertEquals(0, BigDecimal.valueOf(109.99).compareTo(dto.basePrice()));
        assertEquals(15, dto.allocatedStock());
        assertEquals(8, dto.soldCount());
        assertEquals(0, BigDecimal.valueOf(99.99).compareTo(dto.salePrice()));
    }

    @Test
    public void shouldReturnEmptyWhenActiveSaleByItemIdNotFound() {
        Mockito.when(repository.findById(UUID.fromString(itemId1))).thenReturn(Optional.empty());
        assertTrue(service.getActiveSaleByFlashSaleItemId(UUID.fromString(itemId1)).isEmpty());
    }

    @Test
    public void shouldReturnActiveSaleByFlashSaleItemId() {
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = startTime.plusHours(1);
        final RemainingActiveStock stock = new RemainingActiveStock(
            UUID.fromString(itemId1),
            UUID.fromString(saleId1),
            "Sale 1",
            startTime,
            endTime,
            UUID.fromString(productId1),
            10,
            5,
            BigDecimal.valueOf(89.99),
            "Product 1",
            "Desc 1",
            BigDecimal.valueOf(99.99));
        Mockito.when(repository.findById(UUID.fromString(itemId1))).thenReturn(Optional.of(stock));
        final var result = service.getActiveSaleByFlashSaleItemId(UUID.fromString(itemId1));
        assertTrue(result.isPresent());
        assertEquals(itemId1, result.get().flashSaleItemId());
        assertEquals("Product 1", result.get().productName());
    }
}
