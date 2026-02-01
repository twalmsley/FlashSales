package uk.co.aosd.flash.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
import uk.co.aosd.flash.exc.FlashSaleNotFoundException;
import uk.co.aosd.flash.repository.FlashSaleItemRepository;
import uk.co.aosd.flash.repository.FlashSaleRepository;
import uk.co.aosd.flash.repository.ProductRepository;

/**
 * Tests for the FlashSalesService cancelFlashSale method.
 */
public class FlashSalesServiceCancelFlashSaleTest {

    private static FlashSalesService service;

    private static FlashSaleRepository sales;

    private static FlashSaleItemRepository items;

    private static ProductRepository products;

    /**
     * Set up the mocks and the service for testing.
     */
    private static AuditLogService auditLogService;

    @BeforeAll
    public static void beforeAll() {
        sales = Mockito.mock(FlashSaleRepository.class);
        items = Mockito.mock(FlashSaleItemRepository.class);
        products = Mockito.mock(ProductRepository.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        service = new FlashSalesService(sales, items, products, auditLogService);
    }

    /**
     * Reset the mocks.
     */
    @BeforeEach
    public void beforeEach() {
        Mockito.reset(sales);
        Mockito.reset(items);
        Mockito.reset(products);
    }

    @Test
    public void shouldCancelDraftSaleAndReleaseAllStock() {
        final UUID saleId = UUID.randomUUID();

        // Create a product with reserved stock
        final var product = new Product(UUID.randomUUID(), "Test Product", "Description", 100,
            BigDecimal.valueOf(50.0), 30);

        // Create a DRAFT sale with allocated stock (no sales yet)
        final var sale = new FlashSale(saleId, "Draft Sale",
            OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(2),
            SaleStatus.DRAFT, new ArrayList<>());
        final var saleItem = new FlashSaleItem(UUID.randomUUID(), sale, product, 20, 0,
            BigDecimal.valueOf(40.0));
        sale.getItems().add(saleItem);

        when(sales.findById(saleId)).thenReturn(Optional.of(sale));
        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(items.save(any(FlashSaleItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelFlashSale(saleId);

        assertEquals(SaleStatus.CANCELLED, sale.getStatus());
        // Verify allocatedStock was reduced to 0 (soldCount for DRAFT)
        assertEquals(0, saleItem.getAllocatedStock());
        // Verify product reservedCount was decreased by 20 (all allocated stock)
        assertEquals(10, product.getReservedCount()); // 30 - 20 = 10
        verify(sales, times(1)).save(sale);
        verify(items, times(1)).save(saleItem);
        verify(products, times(1)).save(product);
    }

    @Test
    public void shouldCancelActiveSaleWithUnsoldStock() {
        final UUID saleId = UUID.randomUUID();

        // Create a product with reserved stock
        final var product = new Product(UUID.randomUUID(), "Test Product", "Description", 100,
            BigDecimal.valueOf(50.0), 30);

        // Create an ACTIVE sale with unsold stock (allocated 20, sold 10)
        final var sale = new FlashSale(saleId, "Active Sale",
            OffsetDateTime.now().minusHours(1), OffsetDateTime.now().plusHours(1),
            SaleStatus.ACTIVE, new ArrayList<>());
        final var saleItem = new FlashSaleItem(UUID.randomUUID(), sale, product, 20, 10,
            BigDecimal.valueOf(40.0));
        sale.getItems().add(saleItem);

        when(sales.findById(saleId)).thenReturn(Optional.of(sale));
        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(items.save(any(FlashSaleItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelFlashSale(saleId);

        assertEquals(SaleStatus.CANCELLED, sale.getStatus());
        // Verify allocatedStock was reduced to match soldCount (10)
        assertEquals(10, saleItem.getAllocatedStock());
        // Verify product reservedCount was decreased by the difference (20 - 10 = 10)
        assertEquals(20, product.getReservedCount()); // 30 - 10 = 20
        verify(sales, times(1)).save(sale);
        verify(items, times(1)).save(saleItem);
        verify(products, times(1)).save(product);
    }

    @Test
    public void shouldCancelActiveSaleWithAllStockSold() {
        final UUID saleId = UUID.randomUUID();

        // Create a product
        final var product = new Product(UUID.randomUUID(), "Test Product", "Description", 100,
            BigDecimal.valueOf(50.0), 20);

        // Create an ACTIVE sale where all stock was sold (allocated 20, sold 20)
        final var sale = new FlashSale(saleId, "Active Sale",
            OffsetDateTime.now().minusHours(1), OffsetDateTime.now().plusHours(1),
            SaleStatus.ACTIVE, new ArrayList<>());
        final var saleItem = new FlashSaleItem(UUID.randomUUID(), sale, product, 20, 20,
            BigDecimal.valueOf(40.0));
        sale.getItems().add(saleItem);

        when(sales.findById(saleId)).thenReturn(Optional.of(sale));
        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelFlashSale(saleId);

        assertEquals(SaleStatus.CANCELLED, sale.getStatus());
        // Verify allocatedStock was not changed (all stock was sold, difference = 0)
        assertEquals(20, saleItem.getAllocatedStock());
        // Verify product reservedCount was not changed (no unsold stock to release)
        assertEquals(20, product.getReservedCount());
        verify(sales, times(1)).save(sale);
        // Verify item and product were not saved (no changes needed)
        verify(items, times(0)).save(any(FlashSaleItem.class));
        verify(products, times(0)).save(any(Product.class));
    }

    @Test
    public void shouldThrowExceptionWhenCancellingCompletedSale() {
        final UUID saleId = UUID.randomUUID();

        final var sale = new FlashSale(saleId, "Completed Sale",
            OffsetDateTime.now().minusDays(2), OffsetDateTime.now().minusDays(1),
            SaleStatus.COMPLETED, new ArrayList<>());

        when(sales.findById(saleId)).thenReturn(Optional.of(sale));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.cancelFlashSale(saleId));

        assertEquals("Cannot cancel a COMPLETED sale", exception.getMessage());
        assertEquals(SaleStatus.COMPLETED, sale.getStatus()); // Status should not change
        verify(sales, times(0)).save(any(FlashSale.class));
    }

    @Test
    public void shouldThrowExceptionWhenCancellingAlreadyCancelledSale() {
        final UUID saleId = UUID.randomUUID();

        final var sale = new FlashSale(saleId, "Cancelled Sale",
            OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(2),
            SaleStatus.CANCELLED, new ArrayList<>());

        when(sales.findById(saleId)).thenReturn(Optional.of(sale));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.cancelFlashSale(saleId));

        assertEquals("Sale is already CANCELLED", exception.getMessage());
        assertEquals(SaleStatus.CANCELLED, sale.getStatus()); // Status should not change
        verify(sales, times(0)).save(any(FlashSale.class));
    }

    @Test
    public void shouldThrowExceptionWhenSaleNotFound() {
        final UUID saleId = UUID.randomUUID();

        when(sales.findById(saleId)).thenReturn(Optional.empty());

        final FlashSaleNotFoundException exception = assertThrows(FlashSaleNotFoundException.class,
            () -> service.cancelFlashSale(saleId));

        assertEquals(saleId, exception.getSaleId());
        verify(sales, times(0)).save(any(FlashSale.class));
    }

    @Test
    public void shouldProcessMultipleItemsInSale() {
        final UUID saleId = UUID.randomUUID();

        // Create products
        final var product1 = new Product(UUID.randomUUID(), "Product 1", "Description", 100,
            BigDecimal.valueOf(50.0), 30);
        final var product2 = new Product(UUID.randomUUID(), "Product 2", "Description", 200,
            BigDecimal.valueOf(75.0), 50);

        // Create a DRAFT sale with multiple items
        final var sale = new FlashSale(saleId, "Draft Sale",
            OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(2),
            SaleStatus.DRAFT, new ArrayList<>());

        // Item 1: allocated 20, sold 0 (20 unsold - all should be released)
        final var saleItem1 = new FlashSaleItem(UUID.randomUUID(), sale, product1, 20, 0,
            BigDecimal.valueOf(40.0));
        // Item 2: allocated 30, sold 15 (15 unsold)
        final var saleItem2 = new FlashSaleItem(UUID.randomUUID(), sale, product2, 30, 15,
            BigDecimal.valueOf(60.0));
        // Item 3: allocated 10, sold 10 (0 unsold - should be skipped)
        final var saleItem3 = new FlashSaleItem(UUID.randomUUID(), sale, product2, 10, 10,
            BigDecimal.valueOf(60.0));

        sale.getItems().add(saleItem1);
        sale.getItems().add(saleItem2);
        sale.getItems().add(saleItem3);

        when(sales.findById(saleId)).thenReturn(Optional.of(sale));
        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(items.save(any(FlashSaleItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelFlashSale(saleId);

        assertEquals(SaleStatus.CANCELLED, sale.getStatus());

        // Verify item 1: allocatedStock reduced to 0, product1 reservedCount reduced by
        // 20
        assertEquals(0, saleItem1.getAllocatedStock());
        assertEquals(10, product1.getReservedCount()); // 30 - 20 = 10

        // Verify item 2: allocatedStock reduced to 15, product2 reservedCount reduced
        // by 15
        assertEquals(15, saleItem2.getAllocatedStock());
        assertEquals(35, product2.getReservedCount()); // 50 - 15 = 35

        // Verify item 3: no changes (all stock was sold)
        assertEquals(10, saleItem3.getAllocatedStock());

        // Verify saves: 2 items and 2 products (item3 was not saved)
        verify(items, times(2)).save(any(FlashSaleItem.class));
        verify(products, times(2)).save(any(Product.class));
    }

    @Test
    public void shouldHandleSaleWithNoItems() {
        final UUID saleId = UUID.randomUUID();

        // Create a sale with no items
        final var sale = new FlashSale(saleId, "Draft Sale",
            OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(2),
            SaleStatus.DRAFT, new ArrayList<>());

        when(sales.findById(saleId)).thenReturn(Optional.of(sale));
        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelFlashSale(saleId);

        assertEquals(SaleStatus.CANCELLED, sale.getStatus());
        // Verify no item or product saves were called
        verify(items, times(0)).save(any(FlashSaleItem.class));
        verify(products, times(0)).save(any(Product.class));
    }

}
