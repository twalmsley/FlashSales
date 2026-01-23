package uk.co.aosd.flash.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.co.aosd.flash.domain.FlashSale;
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
    @BeforeAll
    public static void beforeAll() {
        sales = Mockito.mock(FlashSaleRepository.class);
        items = Mockito.mock(FlashSaleItemRepository.class);
        products = Mockito.mock(ProductRepository.class);
        service = new FlashSalesService(sales, items, products);
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

}
