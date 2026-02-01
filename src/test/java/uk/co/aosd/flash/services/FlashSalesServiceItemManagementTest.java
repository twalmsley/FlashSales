package uk.co.aosd.flash.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import uk.co.aosd.flash.dto.AddFlashSaleItemDto;
import uk.co.aosd.flash.dto.FlashSaleResponseDto;
import uk.co.aosd.flash.dto.UpdateFlashSaleItemDto;
import uk.co.aosd.flash.exc.FlashSaleItemNotFoundException;
import uk.co.aosd.flash.exc.FlashSaleNotFoundException;
import uk.co.aosd.flash.exc.InsufficientResourcesException;
import uk.co.aosd.flash.exc.ProductNotFoundException;
import uk.co.aosd.flash.repository.FlashSaleItemRepository;
import uk.co.aosd.flash.repository.FlashSaleRepository;
import uk.co.aosd.flash.repository.ProductRepository;

/**
 * Tests for FlashSalesService item management methods.
 */
public class FlashSalesServiceItemManagementTest {

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

    // addItemsToFlashSale tests

    @Test
    public void shouldAddSingleItemToDraftSale() {
        final UUID saleId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final var product = new Product(productId, "Test Product", "Description", 100,
            BigDecimal.valueOf(50.0), 20);
        final var sale = createTestSale(saleId, "Draft Sale", SaleStatus.DRAFT);
        final var itemDto = new AddFlashSaleItemDto(productId.toString(), 30, null);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));
        when(products.findById(productId)).thenReturn(Optional.of(product));
        when(items.existsByFlashSaleIdAndProductId(saleId, productId)).thenReturn(false);
        when(items.save(any(FlashSaleItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final FlashSaleResponseDto result = service.addItemsToFlashSale(saleId, List.of(itemDto));

        assertNotNull(result);
        assertEquals(50, product.getReservedCount()); // 20 + 30 = 50
        verify(items, times(1)).save(any(FlashSaleItem.class));
        verify(products, times(1)).save(product);
    }

    @Test
    public void shouldAddMultipleItemsToDraftSale() {
        final UUID saleId = UUID.randomUUID();
        final UUID productId1 = UUID.randomUUID();
        final UUID productId2 = UUID.randomUUID();
        final var product1 = new Product(productId1, "Product 1", "Description", 100,
            BigDecimal.valueOf(50.0), 10);
        final var product2 = new Product(productId2, "Product 2", "Description", 200,
            BigDecimal.valueOf(75.0), 20);
        final var sale = createTestSale(saleId, "Draft Sale", SaleStatus.DRAFT);
        final var itemDto1 = new AddFlashSaleItemDto(productId1.toString(), 30, BigDecimal.valueOf(40.0));
        final var itemDto2 = new AddFlashSaleItemDto(productId2.toString(), 50, null);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));
        when(products.findById(productId1)).thenReturn(Optional.of(product1));
        when(products.findById(productId2)).thenReturn(Optional.of(product2));
        when(items.existsByFlashSaleIdAndProductId(saleId, productId1)).thenReturn(false);
        when(items.existsByFlashSaleIdAndProductId(saleId, productId2)).thenReturn(false);
        when(items.save(any(FlashSaleItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final FlashSaleResponseDto result = service.addItemsToFlashSale(saleId, List.of(itemDto1, itemDto2));

        assertNotNull(result);
        assertEquals(40, product1.getReservedCount()); // 10 + 30 = 40
        assertEquals(70, product2.getReservedCount()); // 20 + 50 = 70
        verify(items, times(2)).save(any(FlashSaleItem.class));
        verify(products, times(2)).save(any(Product.class));
    }

    @Test
    public void shouldThrowFlashSaleNotFoundExceptionWhenSaleNotFound() {
        final UUID saleId = UUID.randomUUID();
        final var itemDto = new AddFlashSaleItemDto(UUID.randomUUID().toString(), 30, null);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.empty());

        assertThrows(FlashSaleNotFoundException.class,
            () -> service.addItemsToFlashSale(saleId, List.of(itemDto)));
        verify(items, never()).save(any(FlashSaleItem.class));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionForNonDraftSale() {
        final UUID saleId = UUID.randomUUID();
        final var sale = createTestSale(saleId, "Active Sale", SaleStatus.ACTIVE);
        final var itemDto = new AddFlashSaleItemDto(UUID.randomUUID().toString(), 30, null);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.addItemsToFlashSale(saleId, List.of(itemDto)));
        assertEquals("Only DRAFT flash sales can have items added", exception.getMessage());
        verify(items, never()).save(any(FlashSaleItem.class));
    }

    @Test
    public void shouldThrowProductNotFoundExceptionWhenProductNotFound() {
        final UUID saleId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final var sale = createTestSale(saleId, "Draft Sale", SaleStatus.DRAFT);
        final var itemDto = new AddFlashSaleItemDto(productId.toString(), 30, null);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));
        when(products.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
            () -> service.addItemsToFlashSale(saleId, List.of(itemDto)));
        verify(items, never()).save(any(FlashSaleItem.class));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionForDuplicateProduct() {
        final UUID saleId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final var product = new Product(productId, "Test Product", "Description", 100,
            BigDecimal.valueOf(50.0), 20);
        final var sale = createTestSale(saleId, "Draft Sale", SaleStatus.DRAFT);
        final var itemDto = new AddFlashSaleItemDto(productId.toString(), 30, null);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));
        when(products.findById(productId)).thenReturn(Optional.of(product));
        when(items.existsByFlashSaleIdAndProductId(saleId, productId)).thenReturn(true);

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.addItemsToFlashSale(saleId, List.of(itemDto)));
        assertEquals("Products already in sale: " + productId, exception.getMessage());
        verify(items, never()).save(any(FlashSaleItem.class));
    }

    @Test
    public void shouldThrowInsufficientResourcesExceptionForInsufficientStock() {
        final UUID saleId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final var product = new Product(productId, "Test Product", "Description", 100,
            BigDecimal.valueOf(50.0), 90); // Already has 90 reserved
        final var sale = createTestSale(saleId, "Draft Sale", SaleStatus.DRAFT);
        final var itemDto = new AddFlashSaleItemDto(productId.toString(), 20, null); // Would exceed 100

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));
        when(products.findById(productId)).thenReturn(Optional.of(product));
        when(items.existsByFlashSaleIdAndProductId(saleId, productId)).thenReturn(false);

        assertThrows(InsufficientResourcesException.class,
            () -> service.addItemsToFlashSale(saleId, List.of(itemDto)));
        verify(items, never()).save(any(FlashSaleItem.class));
    }

    // updateFlashSaleItem tests

    @Test
    public void shouldUpdateAllocatedStockOnly() {
        final UUID saleId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final var product = new Product(productId, "Test Product", "Description", 100,
            BigDecimal.valueOf(50.0), 50);
        final var sale = createTestSale(saleId, "Draft Sale", SaleStatus.DRAFT);
        final var item = new FlashSaleItem(itemId, sale, product, 30, 0, BigDecimal.valueOf(40.0));
        final var updateDto = new UpdateFlashSaleItemDto(50, null);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));
        when(items.findByIdAndFlashSaleId(itemId, saleId)).thenReturn(Optional.of(item));
        when(items.save(any(FlashSaleItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final FlashSaleResponseDto result = service.updateFlashSaleItem(saleId, itemId, updateDto);

        assertNotNull(result);
        assertEquals(50, item.getAllocatedStock());
        assertEquals(70, product.getReservedCount()); // 50 + 20 = 70
        verify(items, times(1)).save(item);
        verify(products, times(1)).save(product);
    }

    @Test
    public void shouldUpdateSalePriceOnly() {
        final UUID saleId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final var product = new Product(productId, "Test Product", "Description", 100,
            BigDecimal.valueOf(50.0), 30);
        final var sale = createTestSale(saleId, "Draft Sale", SaleStatus.DRAFT);
        final var item = new FlashSaleItem(itemId, sale, product, 30, 0, BigDecimal.valueOf(40.0));
        final var updateDto = new UpdateFlashSaleItemDto(null, BigDecimal.valueOf(35.0));

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));
        when(items.findByIdAndFlashSaleId(itemId, saleId)).thenReturn(Optional.of(item));
        when(items.save(any(FlashSaleItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final FlashSaleResponseDto result = service.updateFlashSaleItem(saleId, itemId, updateDto);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(35.0), item.getSalePrice());
        assertEquals(30, product.getReservedCount()); // Unchanged
        verify(items, times(1)).save(item);
        verify(products, never()).save(any(Product.class));
    }

    @Test
    public void shouldUpdateBothAllocatedStockAndSalePrice() {
        final UUID saleId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final var product = new Product(productId, "Test Product", "Description", 100,
            BigDecimal.valueOf(50.0), 40);
        final var sale = createTestSale(saleId, "Draft Sale", SaleStatus.DRAFT);
        final var item = new FlashSaleItem(itemId, sale, product, 30, 0, BigDecimal.valueOf(40.0));
        final var updateDto = new UpdateFlashSaleItemDto(50, BigDecimal.valueOf(35.0));

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));
        when(items.findByIdAndFlashSaleId(itemId, saleId)).thenReturn(Optional.of(item));
        when(items.save(any(FlashSaleItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final FlashSaleResponseDto result = service.updateFlashSaleItem(saleId, itemId, updateDto);

        assertNotNull(result);
        assertEquals(50, item.getAllocatedStock());
        assertEquals(BigDecimal.valueOf(35.0), item.getSalePrice());
        assertEquals(60, product.getReservedCount()); // 40 + 20 = 60
        verify(items, times(1)).save(item);
        verify(products, times(1)).save(product);
    }

    @Test
    public void shouldThrowFlashSaleNotFoundExceptionWhenUpdatingItemInNonExistentSale() {
        final UUID saleId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        final var updateDto = new UpdateFlashSaleItemDto(50, null);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.empty());

        assertThrows(FlashSaleNotFoundException.class,
            () -> service.updateFlashSaleItem(saleId, itemId, updateDto));
        verify(items, never()).save(any(FlashSaleItem.class));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionForNonDraftSaleWhenUpdating() {
        final UUID saleId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        final var sale = createTestSale(saleId, "Active Sale", SaleStatus.ACTIVE);
        final var updateDto = new UpdateFlashSaleItemDto(50, null);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.updateFlashSaleItem(saleId, itemId, updateDto));
        assertEquals("Only DRAFT flash sales can have items updated", exception.getMessage());
        verify(items, never()).save(any(FlashSaleItem.class));
    }

    @Test
    public void shouldThrowFlashSaleItemNotFoundExceptionWhenUpdatingNonExistentItem() {
        final UUID saleId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        final var sale = createTestSale(saleId, "Draft Sale", SaleStatus.DRAFT);
        final var updateDto = new UpdateFlashSaleItemDto(50, null);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));
        when(items.findByIdAndFlashSaleId(itemId, saleId)).thenReturn(Optional.empty());

        assertThrows(FlashSaleItemNotFoundException.class,
            () -> service.updateFlashSaleItem(saleId, itemId, updateDto));
        verify(items, never()).save(any(FlashSaleItem.class));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenAllocatedStockBelowSoldCount() {
        final UUID saleId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final var product = new Product(productId, "Test Product", "Description", 100,
            BigDecimal.valueOf(50.0), 30);
        final var sale = createTestSale(saleId, "Draft Sale", SaleStatus.DRAFT);
        final var item = new FlashSaleItem(itemId, sale, product, 30, 10, BigDecimal.valueOf(40.0)); // soldCount = 10
        final var updateDto = new UpdateFlashSaleItemDto(5, null); // Less than soldCount

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));
        when(items.findByIdAndFlashSaleId(itemId, saleId)).thenReturn(Optional.of(item));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.updateFlashSaleItem(saleId, itemId, updateDto));
        assertEquals("Allocated stock (5) cannot be less than sold count (10)", exception.getMessage());
        verify(items, never()).save(any(FlashSaleItem.class));
    }

    @Test
    public void shouldAllowAllocatedStockEqualToSoldCount() {
        final UUID saleId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final var product = new Product(productId, "Test Product", "Description", 100,
            BigDecimal.valueOf(50.0), 30);
        final var sale = createTestSale(saleId, "Draft Sale", SaleStatus.DRAFT);
        final var item = new FlashSaleItem(itemId, sale, product, 30, 10, BigDecimal.valueOf(40.0));
        final var updateDto = new UpdateFlashSaleItemDto(10, null); // Equal to soldCount

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));
        when(items.findByIdAndFlashSaleId(itemId, saleId)).thenReturn(Optional.of(item));
        when(items.save(any(FlashSaleItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final FlashSaleResponseDto result = service.updateFlashSaleItem(saleId, itemId, updateDto);

        assertNotNull(result);
        assertEquals(10, item.getAllocatedStock());
        assertEquals(10, product.getReservedCount()); // 30 - 20 = 10
        verify(items, times(1)).save(item);
        verify(products, times(1)).save(product);
    }

    @Test
    public void shouldThrowInsufficientResourcesExceptionWhenIncreasingStockBeyondAvailable() {
        final UUID saleId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final var product = new Product(productId, "Test Product", "Description", 100,
            BigDecimal.valueOf(50.0), 90); // Already has 90 reserved
        final var sale = createTestSale(saleId, "Draft Sale", SaleStatus.DRAFT);
        final var item = new FlashSaleItem(itemId, sale, product, 10, 0, BigDecimal.valueOf(40.0));
        final var updateDto = new UpdateFlashSaleItemDto(30, null); // Would exceed 100

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));
        when(items.findByIdAndFlashSaleId(itemId, saleId)).thenReturn(Optional.of(item));

        assertThrows(InsufficientResourcesException.class,
            () -> service.updateFlashSaleItem(saleId, itemId, updateDto));
        verify(items, never()).save(any(FlashSaleItem.class));
    }

    // removeFlashSaleItem tests

    @Test
    public void shouldRemoveItemWithNoSales() {
        final UUID saleId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final var product = new Product(productId, "Test Product", "Description", 100,
            BigDecimal.valueOf(50.0), 30);
        final var sale = createTestSale(saleId, "Draft Sale", SaleStatus.DRAFT);
        final var item = new FlashSaleItem(itemId, sale, product, 20, 0, BigDecimal.valueOf(40.0));
        sale.getItems().add(item);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final FlashSaleResponseDto result = service.removeFlashSaleItem(saleId, itemId);

        assertNotNull(result);
        assertEquals(10, product.getReservedCount()); // 30 - 20 = 10
        verify(products, times(1)).save(product);
        verify(sales, times(1)).save(sale);
        verify(sales, times(1)).flush();
    }

    @Test
    public void shouldRemoveItemWithSomeSales() {
        final UUID saleId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final var product = new Product(productId, "Test Product", "Description", 100,
            BigDecimal.valueOf(50.0), 30);
        final var sale = createTestSale(saleId, "Draft Sale", SaleStatus.DRAFT);
        final var item = new FlashSaleItem(itemId, sale, product, 20, 5, BigDecimal.valueOf(40.0)); // 5 sold
        sale.getItems().add(item);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final FlashSaleResponseDto result = service.removeFlashSaleItem(saleId, itemId);

        assertNotNull(result);
        assertEquals(15, product.getReservedCount()); // 30 - (20 - 5) = 15
        verify(products, times(1)).save(product);
        verify(sales, times(1)).save(sale);
        verify(sales, times(1)).flush();
    }

    @Test
    public void shouldThrowFlashSaleNotFoundExceptionWhenRemovingItemFromNonExistentSale() {
        final UUID saleId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.empty());

        assertThrows(FlashSaleNotFoundException.class,
            () -> service.removeFlashSaleItem(saleId, itemId));
        verify(items, never()).delete(any(FlashSaleItem.class));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionForNonDraftSaleWhenRemoving() {
        final UUID saleId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        final var sale = createTestSale(saleId, "Active Sale", SaleStatus.ACTIVE);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.removeFlashSaleItem(saleId, itemId));
        assertEquals("Only DRAFT flash sales can have items removed", exception.getMessage());
        verify(items, never()).delete(any(FlashSaleItem.class));
    }

    @Test
    public void shouldThrowFlashSaleItemNotFoundExceptionWhenRemovingNonExistentItem() {
        final UUID saleId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        final var sale = createTestSale(saleId, "Draft Sale", SaleStatus.DRAFT);

        when(sales.findByIdWithItems(saleId)).thenReturn(Optional.of(sale));
        when(items.findByIdAndFlashSaleId(itemId, saleId)).thenReturn(Optional.empty());

        assertThrows(FlashSaleItemNotFoundException.class,
            () -> service.removeFlashSaleItem(saleId, itemId));
        verify(items, never()).delete(any(FlashSaleItem.class));
    }

    // Helper methods

    private FlashSale createTestSale(final UUID id, final String title, final SaleStatus status) {
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        final FlashSale sale = new FlashSale(id, title, startTime, endTime, status, new ArrayList<>());
        return sale;
    }
}
