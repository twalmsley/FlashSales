package uk.co.aosd.flash.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.co.aosd.flash.domain.FlashSale;
import uk.co.aosd.flash.domain.FlashSaleItem;
import uk.co.aosd.flash.domain.Product;
import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.repository.FlashSaleItemRepository;
import uk.co.aosd.flash.repository.FlashSaleRepository;
import uk.co.aosd.flash.repository.ProductRepository;

/**
 * Tests for the FlashSalesService completeActiveSales method.
 */
public class FlashSalesServiceCompleteActiveSalesTest {

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
    public void shouldCompleteActiveSalesWhenEndTimeHasPassed() {
        final var now = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        
        // Create active sales ready to complete
        final var activeSale1 = new FlashSale(UUID.randomUUID(), "Active Sale 1", 
            now.minusHours(2), now.minusHours(1), SaleStatus.ACTIVE, List.of());
        final var activeSale2 = new FlashSale(UUID.randomUUID(), "Active Sale 2", 
            now.minusHours(3), now.minusMinutes(30), SaleStatus.ACTIVE, List.of());
        final var activeSale3 = new FlashSale(UUID.randomUUID(), "Active Sale 3", 
            now.minusHours(1), now, SaleStatus.ACTIVE, List.of());

        when(sales.findActiveSalesReadyToComplete(eq(SaleStatus.ACTIVE), any(OffsetDateTime.class)))
            .thenReturn(List.of(activeSale1, activeSale2, activeSale3));

        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final int completedCount = service.completeActiveSales();

        assertEquals(3, completedCount);
        assertEquals(SaleStatus.COMPLETED, activeSale1.getStatus());
        assertEquals(SaleStatus.COMPLETED, activeSale2.getStatus());
        assertEquals(SaleStatus.COMPLETED, activeSale3.getStatus());
        verify(sales, times(3)).save(any(FlashSale.class));
    }

    @Test
    public void shouldReturnZeroWhenNoActiveSalesReadyToComplete() {
        when(sales.findActiveSalesReadyToComplete(eq(SaleStatus.ACTIVE), any(OffsetDateTime.class)))
            .thenReturn(List.of());

        final int completedCount = service.completeActiveSales();

        assertEquals(0, completedCount);
        verify(sales, times(0)).save(any(FlashSale.class));
    }

    @Test
    public void shouldOnlyCompleteActiveSalesWithEndTimePassed() {
        final var now = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        
        // Create an active sale ready to complete
        final var activeSale1 = new FlashSale(UUID.randomUUID(), "Active Sale Ready", 
            now.minusHours(2), now.minusHours(1), SaleStatus.ACTIVE, List.of());
        
        // Create an active sale that shouldn't be completed (status changed externally between query and processing)
        // This tests the defensive check in the service method
        final var activeSale2 = new FlashSale(UUID.randomUUID(), "Active Sale Changed", 
            now.minusHours(2), now.minusHours(1), SaleStatus.ACTIVE, List.of());
        // Simulate status change after query but before processing
        activeSale2.setStatus(SaleStatus.COMPLETED);

        when(sales.findActiveSalesReadyToComplete(eq(SaleStatus.ACTIVE), any(OffsetDateTime.class)))
            .thenReturn(List.of(activeSale1, activeSale2));

        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final int completedCount = service.completeActiveSales();

        assertEquals(1, completedCount);
        assertEquals(SaleStatus.COMPLETED, activeSale1.getStatus());
        assertEquals(SaleStatus.COMPLETED, activeSale2.getStatus()); // Already completed, defensive check prevented re-completion
        verify(sales, times(1)).save(activeSale1);
    }

    @Test
    public void shouldReleaseUnsoldStockWhenSaleIsCompleted() {
        final var now = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        
        // Create a product with reserved stock
        final var product = new Product(UUID.randomUUID(), "Test Product", "Description", 100, 
            BigDecimal.valueOf(50.0), 30);
        
        // Create a sale item with unsold stock (allocated 20, sold 10)
        final var sale = new FlashSale(UUID.randomUUID(), "Active Sale", 
            now.minusHours(2), now.minusHours(1), SaleStatus.ACTIVE, new ArrayList<>());
        final var saleItem = new FlashSaleItem(UUID.randomUUID(), sale, product, 20, 10, 
            BigDecimal.valueOf(40.0));
        sale.getItems().add(saleItem);
        
        when(sales.findActiveSalesReadyToComplete(eq(SaleStatus.ACTIVE), any(OffsetDateTime.class)))
            .thenReturn(List.of(sale));
        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(items.save(any(FlashSaleItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        final int completedCount = service.completeActiveSales();
        
        assertEquals(1, completedCount);
        assertEquals(SaleStatus.COMPLETED, sale.getStatus());
        // Verify allocatedStock was reduced to match soldCount
        assertEquals(10, saleItem.getAllocatedStock());
        // Verify product reservedCount was decreased by the difference (20 - 10 = 10)
        assertEquals(20, product.getReservedCount()); // 30 - 10 = 20
        verify(items, times(1)).save(saleItem);
        verify(products, times(1)).save(product);
    }

    @Test
    public void shouldSkipItemsWithNoUnsoldStock() {
        final var now = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        
        // Create a product
        final var product = new Product(UUID.randomUUID(), "Test Product", "Description", 100, 
            BigDecimal.valueOf(50.0), 20);
        
        // Create a sale item where all stock was sold (allocated 20, sold 20)
        final var sale = new FlashSale(UUID.randomUUID(), "Active Sale", 
            now.minusHours(2), now.minusHours(1), SaleStatus.ACTIVE, new ArrayList<>());
        final var saleItem = new FlashSaleItem(UUID.randomUUID(), sale, product, 20, 20, 
            BigDecimal.valueOf(40.0));
        sale.getItems().add(saleItem);
        
        when(sales.findActiveSalesReadyToComplete(eq(SaleStatus.ACTIVE), any(OffsetDateTime.class)))
            .thenReturn(List.of(sale));
        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        final int completedCount = service.completeActiveSales();
        
        assertEquals(1, completedCount);
        assertEquals(SaleStatus.COMPLETED, sale.getStatus());
        // Verify allocatedStock was not changed (all stock was sold)
        assertEquals(20, saleItem.getAllocatedStock());
        // Verify product reservedCount was not changed
        assertEquals(20, product.getReservedCount());
        // Verify item and product were not saved (no changes needed)
        verify(items, times(0)).save(any(FlashSaleItem.class));
        verify(products, times(0)).save(any(Product.class));
    }

    @Test
    public void shouldProcessMultipleItemsInSale() {
        final var now = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        
        // Create products
        final var product1 = new Product(UUID.randomUUID(), "Product 1", "Description", 100, 
            BigDecimal.valueOf(50.0), 30);
        final var product2 = new Product(UUID.randomUUID(), "Product 2", "Description", 200, 
            BigDecimal.valueOf(75.0), 50);
        
        // Create a sale with multiple items
        final var sale = new FlashSale(UUID.randomUUID(), "Active Sale", 
            now.minusHours(2), now.minusHours(1), SaleStatus.ACTIVE, new ArrayList<>());
        
        // Item 1: allocated 20, sold 10 (10 unsold)
        final var saleItem1 = new FlashSaleItem(UUID.randomUUID(), sale, product1, 20, 10, 
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
        
        when(sales.findActiveSalesReadyToComplete(eq(SaleStatus.ACTIVE), any(OffsetDateTime.class)))
            .thenReturn(List.of(sale));
        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(items.save(any(FlashSaleItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        final int completedCount = service.completeActiveSales();
        
        assertEquals(1, completedCount);
        assertEquals(SaleStatus.COMPLETED, sale.getStatus());
        
        // Verify item 1: allocatedStock reduced to 10, product1 reservedCount reduced by 10
        assertEquals(10, saleItem1.getAllocatedStock());
        assertEquals(20, product1.getReservedCount()); // 30 - 10 = 20
        
        // Verify item 2: allocatedStock reduced to 15, product2 reservedCount reduced by 15
        assertEquals(15, saleItem2.getAllocatedStock());
        assertEquals(35, product2.getReservedCount()); // 50 - 15 = 35
        
        // Verify item 3: no changes (all stock was sold)
        assertEquals(10, saleItem3.getAllocatedStock());
        
        // Verify saves: 2 items and 2 products (item3 and its product were not saved)
        verify(items, times(2)).save(any(FlashSaleItem.class));
        verify(products, times(2)).save(any(Product.class));
    }

    @Test
    public void shouldHandleSaleWithNoItems() {
        final var now = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        
        // Create a sale with no items
        final var sale = new FlashSale(UUID.randomUUID(), "Active Sale", 
            now.minusHours(2), now.minusHours(1), SaleStatus.ACTIVE, new ArrayList<>());
        
        when(sales.findActiveSalesReadyToComplete(eq(SaleStatus.ACTIVE), any(OffsetDateTime.class)))
            .thenReturn(List.of(sale));
        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        final int completedCount = service.completeActiveSales();
        
        assertEquals(1, completedCount);
        assertEquals(SaleStatus.COMPLETED, sale.getStatus());
        // Verify no item or product saves were called
        verify(items, times(0)).save(any(FlashSaleItem.class));
        verify(products, times(0)).save(any(Product.class));
    }

}
