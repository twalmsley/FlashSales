package uk.co.aosd.flash.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.co.aosd.flash.domain.FlashSale;
import uk.co.aosd.flash.domain.FlashSaleItem;
import uk.co.aosd.flash.domain.Product;
import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.FlashSaleResponseDto;
import uk.co.aosd.flash.dto.UpdateFlashSaleDto;
import uk.co.aosd.flash.exc.FlashSaleNotFoundException;
import uk.co.aosd.flash.exc.InvalidSaleTimesException;
import uk.co.aosd.flash.exc.SaleDurationTooShortException;
import uk.co.aosd.flash.repository.FlashSaleItemRepository;
import uk.co.aosd.flash.repository.FlashSaleRepository;
import uk.co.aosd.flash.repository.ProductRepository;

/**
 * Tests for FlashSalesService management methods.
 */
public class FlashSalesServiceManagementTest {

    private static FlashSalesService service;

    private static FlashSaleRepository sales;

    private static FlashSaleItemRepository items;

    private static ProductRepository products;

    private static AuditLogService auditLogService;

    @BeforeAll
    public static void beforeAll() {
        sales = Mockito.mock(FlashSaleRepository.class);
        items = Mockito.mock(FlashSaleItemRepository.class);
        products = Mockito.mock(ProductRepository.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        service = new FlashSalesService(sales, items, products, auditLogService);
    }

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(sales);
        Mockito.reset(items);
        Mockito.reset(products);
    }

    // getAllFlashSales tests

    @Test
    public void shouldReturnAllSalesWhenNoFiltersProvided() {
        final var sale1 = createTestSale(UUID.randomUUID(), "Sale 1", SaleStatus.DRAFT);
        final var sale2 = createTestSale(UUID.randomUUID(), "Sale 2", SaleStatus.ACTIVE);
        final List<FlashSale> allSales = List.of(sale1, sale2);

        when(sales.findAllWithFilters(null, null, null)).thenReturn(allSales);

        final List<FlashSaleResponseDto> result = service.getAllFlashSales(null, null, null);

        assertEquals(2, result.size());
        verify(sales, times(1)).findAllWithFilters(null, null, null);
    }

    @Test
    public void shouldFilterByStatusOnly() {
        final var sale1 = createTestSale(UUID.randomUUID(), "Draft Sale", SaleStatus.DRAFT);
        final List<FlashSale> draftSales = List.of(sale1);

        when(sales.findAllWithFilters(SaleStatus.DRAFT, null, null)).thenReturn(draftSales);

        final List<FlashSaleResponseDto> result = service.getAllFlashSales(SaleStatus.DRAFT, null, null);

        assertEquals(1, result.size());
        assertEquals(SaleStatus.DRAFT, result.get(0).status());
        verify(sales, times(1)).findAllWithFilters(SaleStatus.DRAFT, null, null);
    }

    @Test
    public void shouldFilterByDateRangeOnly() {
        final OffsetDateTime startDate = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endDate = OffsetDateTime.of(2026, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        final var sale1 = createTestSale(UUID.randomUUID(), "Sale 1", SaleStatus.DRAFT);
        final List<FlashSale> filteredSales = List.of(sale1);

        when(sales.findAllWithFilters(null, startDate, endDate)).thenReturn(filteredSales);

        final List<FlashSaleResponseDto> result = service.getAllFlashSales(null, startDate, endDate);

        assertEquals(1, result.size());
        verify(sales, times(1)).findAllWithFilters(null, startDate, endDate);
    }

    @Test
    public void shouldFilterByStatusAndDateRange() {
        final OffsetDateTime startDate = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endDate = OffsetDateTime.of(2026, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        final var sale1 = createTestSale(UUID.randomUUID(), "Draft Sale", SaleStatus.DRAFT);
        final List<FlashSale> filteredSales = List.of(sale1);

        when(sales.findAllWithFilters(SaleStatus.DRAFT, startDate, endDate)).thenReturn(filteredSales);

        final List<FlashSaleResponseDto> result = service.getAllFlashSales(SaleStatus.DRAFT, startDate, endDate);

        assertEquals(1, result.size());
        assertEquals(SaleStatus.DRAFT, result.get(0).status());
        verify(sales, times(1)).findAllWithFilters(SaleStatus.DRAFT, startDate, endDate);
    }

    @Test
    public void shouldReturnEmptyListWhenNoMatches() {
        when(sales.findAllWithFilters(SaleStatus.COMPLETED, null, null)).thenReturn(List.of());

        final List<FlashSaleResponseDto> result = service.getAllFlashSales(SaleStatus.COMPLETED, null, null);

        assertTrue(result.isEmpty());
        verify(sales, times(1)).findAllWithFilters(SaleStatus.COMPLETED, null, null);
    }

    // getFlashSaleById tests

    @Test
    public void shouldReturnFlashSaleWhenFound() {
        final UUID saleId = UUID.randomUUID();
        final var sale = createTestSale(saleId, "Test Sale", SaleStatus.DRAFT);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final FlashSaleResponseDto result = service.getFlashSaleById(saleId);

        assertNotNull(result);
        assertEquals(saleId.toString(), result.id());
        assertEquals("Test Sale", result.title());
        verify(sales, times(1)).findByIdWithItems(saleId);
    }

    @Test
    public void shouldThrowFlashSaleNotFoundExceptionWhenNotFound() {
        final UUID saleId = UUID.randomUUID();

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.empty());

        assertThrows(FlashSaleNotFoundException.class, () -> service.getFlashSaleById(saleId));
        verify(sales, times(1)).findByIdWithItems(saleId);
    }

    @Test
    public void shouldEagerlyLoadItemsAndProducts() {
        final UUID saleId = UUID.randomUUID();
        final var product = new Product(UUID.randomUUID(), "Test Product", "Description", 100,
            BigDecimal.valueOf(50.0), 10);
        final var sale = createTestSale(saleId, "Test Sale", SaleStatus.DRAFT);
        final var item = new FlashSaleItem(UUID.randomUUID(), sale, product, 20, 0,
            BigDecimal.valueOf(40.0));
        sale.getItems().add(item);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final FlashSaleResponseDto result = service.getFlashSaleById(saleId);

        assertNotNull(result);
        assertEquals(1, result.items().size());
        assertEquals(product.getId().toString(), result.items().get(0).productId());
        assertEquals("Test Product", result.items().get(0).productName());
    }

    // updateFlashSale tests

    @Test
    public void shouldUpdateTitleOnly() {
        final UUID saleId = UUID.randomUUID();
        final var sale = createTestSale(saleId, "Old Title", SaleStatus.DRAFT);
        final UpdateFlashSaleDto updateDto = new UpdateFlashSaleDto("New Title", null, null);

        when(sales.findById(saleId)).thenReturn(Optional.of(sale));
        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final FlashSaleResponseDto result = service.updateFlashSale(saleId, updateDto);

        assertEquals("New Title", result.title());
        verify(sales, times(1)).save(sale);
    }

    @Test
    public void shouldUpdateStartTimeOnly() {
        final UUID saleId = UUID.randomUUID();
        // Create sale with endTime at 12:00, so new startTime at 10:00 is valid
        final var sale = createTestSale(saleId, "Test Sale", SaleStatus.DRAFT);
        // New startTime must be before existing endTime (12:00)
        final OffsetDateTime newStartTime = OffsetDateTime.of(2026, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);
        final UpdateFlashSaleDto updateDto = new UpdateFlashSaleDto(null, newStartTime, null);

        when(sales.findById(saleId)).thenReturn(Optional.of(sale));
        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final FlashSaleResponseDto result = service.updateFlashSale(saleId, updateDto);

        assertEquals(newStartTime, result.startTime());
        verify(sales, times(1)).save(sale);
    }

    @Test
    public void shouldUpdateEndTimeOnly() {
        final UUID saleId = UUID.randomUUID();
        // Create sale with startTime at 10:00, so new endTime at 15:00 is valid
        final var sale = createTestSale(saleId, "Test Sale", SaleStatus.DRAFT);
        // New endTime must be after existing startTime (10:00)
        final OffsetDateTime newEndTime = OffsetDateTime.of(2026, 1, 15, 15, 0, 0, 0, ZoneOffset.UTC);
        final UpdateFlashSaleDto updateDto = new UpdateFlashSaleDto(null, null, newEndTime);

        when(sales.findById(saleId)).thenReturn(Optional.of(sale));
        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final FlashSaleResponseDto result = service.updateFlashSale(saleId, updateDto);

        assertEquals(newEndTime, result.endTime());
        verify(sales, times(1)).save(sale);
    }

    @Test
    public void shouldUpdateAllFields() {
        final UUID saleId = UUID.randomUUID();
        final var sale = createTestSale(saleId, "Old Title", SaleStatus.DRAFT);
        final OffsetDateTime newStartTime = OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime newEndTime = OffsetDateTime.of(2026, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        final UpdateFlashSaleDto updateDto = new UpdateFlashSaleDto("New Title", newStartTime, newEndTime);

        when(sales.findById(saleId)).thenReturn(Optional.of(sale));
        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final FlashSaleResponseDto result = service.updateFlashSale(saleId, updateDto);

        assertEquals("New Title", result.title());
        assertEquals(newStartTime, result.startTime());
        assertEquals(newEndTime, result.endTime());
        verify(sales, times(1)).save(sale);
    }

    @Test
    public void shouldValidateEndTimeAfterStartTime() {
        final UUID saleId = UUID.randomUUID();
        final var sale = createTestSale(saleId, "Test Sale", SaleStatus.DRAFT);
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = OffsetDateTime.of(2026, 1, 15, 11, 0, 0, 0, ZoneOffset.UTC);
        final UpdateFlashSaleDto updateDto = new UpdateFlashSaleDto(null, startTime, endTime);

        when(sales.findById(saleId)).thenReturn(Optional.of(sale));

        assertThrows(InvalidSaleTimesException.class, () -> service.updateFlashSale(saleId, updateDto));
        verify(sales, never()).save(any(FlashSale.class));
    }

    @Test
    public void shouldValidateMinimumDuration() {
        final UUID saleId = UUID.randomUUID();
        final var sale = createTestSale(saleId, "Test Sale", SaleStatus.DRAFT);
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = OffsetDateTime.of(2026, 1, 15, 12, 5, 0, 0, ZoneOffset.UTC); // 5 minutes
        final UpdateFlashSaleDto updateDto = new UpdateFlashSaleDto(null, startTime, endTime);

        when(sales.findById(saleId)).thenReturn(Optional.of(sale));

        assertThrows(SaleDurationTooShortException.class, () -> service.updateFlashSale(saleId, updateDto));
        verify(sales, never()).save(any(FlashSale.class));
    }

    @Test
    public void shouldThrowFlashSaleNotFoundExceptionWhenUpdatingNonExistentSale() {
        final UUID saleId = UUID.randomUUID();
        final UpdateFlashSaleDto updateDto = new UpdateFlashSaleDto("New Title", null, null);

        when(sales.findById(saleId)).thenReturn(Optional.empty());

        assertThrows(FlashSaleNotFoundException.class, () -> service.updateFlashSale(saleId, updateDto));
        verify(sales, never()).save(any(FlashSale.class));
    }

    // deleteFlashSale tests

    @Test
    public void shouldDeleteDraftSaleAndReleaseAllAllocatedStock() {
        final UUID saleId = UUID.randomUUID();
        final var product = new Product(UUID.randomUUID(), "Test Product", "Description", 100,
            BigDecimal.valueOf(50.0), 30);
        final var sale = createTestSale(saleId, "Draft Sale", SaleStatus.DRAFT);
        final var item = new FlashSaleItem(UUID.randomUUID(), sale, product, 20, 0,
            BigDecimal.valueOf(40.0));
        sale.getItems().add(item);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteFlashSale(saleId);

        // Verify product reservedCount was decreased by 20 (all allocated stock)
        assertEquals(10, product.getReservedCount()); // 30 - 20 = 10
        verify(products, times(1)).save(product);
        verify(sales, times(1)).delete(sale);
    }

    @Test
    public void shouldThrowFlashSaleNotFoundExceptionWhenDeletingNonExistentSale() {
        final UUID saleId = UUID.randomUUID();

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.empty());

        assertThrows(FlashSaleNotFoundException.class, () -> service.deleteFlashSale(saleId));
        verify(sales, never()).delete(any(FlashSale.class));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionForActiveStatus() {
        final UUID saleId = UUID.randomUUID();
        final var sale = createTestSale(saleId, "Active Sale", SaleStatus.ACTIVE);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.deleteFlashSale(saleId));
        assertEquals("Only DRAFT flash sales can be deleted", exception.getMessage());
        verify(sales, never()).delete(any(FlashSale.class));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionForCompletedStatus() {
        final UUID saleId = UUID.randomUUID();
        final var sale = createTestSale(saleId, "Completed Sale", SaleStatus.COMPLETED);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.deleteFlashSale(saleId));
        assertEquals("Only DRAFT flash sales can be deleted", exception.getMessage());
        verify(sales, never()).delete(any(FlashSale.class));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionForCancelledStatus() {
        final UUID saleId = UUID.randomUUID();
        final var sale = createTestSale(saleId, "Cancelled Sale", SaleStatus.CANCELLED);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.deleteFlashSale(saleId));
        assertEquals("Only DRAFT flash sales can be deleted", exception.getMessage());
        verify(sales, never()).delete(any(FlashSale.class));
    }

    @Test
    public void shouldVerifyStockReleaseProductReservedCountDecreasesByAllocatedStock() {
        final UUID saleId = UUID.randomUUID();
        final var product = new Product(UUID.randomUUID(), "Test Product", "Description", 100,
            BigDecimal.valueOf(50.0), 50);
        final var sale = createTestSale(saleId, "Draft Sale", SaleStatus.DRAFT);
        final var item = new FlashSaleItem(UUID.randomUUID(), sale, product, 30, 0,
            BigDecimal.valueOf(40.0));
        sale.getItems().add(item);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteFlashSale(saleId);

        // Verify product reservedCount was decreased by 30 (allocatedStock)
        assertEquals(20, product.getReservedCount()); // 50 - 30 = 20
        verify(products, times(1)).save(product);
    }

    // Helper methods

    private FlashSale createTestSale(final UUID id, final String title, final SaleStatus status) {
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        return new FlashSale(id, title, startTime, endTime, status, new ArrayList<>());
    }
}
