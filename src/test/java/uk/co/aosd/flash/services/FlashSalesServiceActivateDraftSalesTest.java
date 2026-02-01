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
 * Tests for the FlashSalesService activateDraftSales method.
 */
public class FlashSalesServiceActivateDraftSalesTest {

    private static FlashSalesService service;

    private static FlashSaleRepository sales;

    private static FlashSaleItemRepository items;

    private static ProductRepository products;

    private static AuditLogService auditLogService;

    /**
     * Set up the mocks and the service for testing.
     */
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
    public void shouldActivateDraftSalesWhenStartTimeHasPassed() {
        final var now = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        
        // Create draft sales ready to activate
        final var draftSale1 = new FlashSale(UUID.randomUUID(), "Draft Sale 1", 
            now.minusHours(1), now.plusHours(1), SaleStatus.DRAFT, List.of());
        final var draftSale2 = new FlashSale(UUID.randomUUID(), "Draft Sale 2", 
            now.minusMinutes(30), now.plusHours(1), SaleStatus.DRAFT, List.of());
        final var draftSale3 = new FlashSale(UUID.randomUUID(), "Draft Sale 3", 
            now, now.plusHours(1), SaleStatus.DRAFT, List.of());

        when(sales.findDraftSalesReadyToActivate(eq(SaleStatus.DRAFT), any(OffsetDateTime.class)))
            .thenReturn(List.of(draftSale1, draftSale2, draftSale3));

        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final int activatedCount = service.activateDraftSales();

        assertEquals(3, activatedCount);
        assertEquals(SaleStatus.ACTIVE, draftSale1.getStatus());
        assertEquals(SaleStatus.ACTIVE, draftSale2.getStatus());
        assertEquals(SaleStatus.ACTIVE, draftSale3.getStatus());
        verify(sales, times(3)).save(any(FlashSale.class));
    }

    @Test
    public void shouldReturnZeroWhenNoDraftSalesReadyToActivate() {
        when(sales.findDraftSalesReadyToActivate(eq(SaleStatus.DRAFT), any(OffsetDateTime.class)))
            .thenReturn(List.of());

        final int activatedCount = service.activateDraftSales();

        assertEquals(0, activatedCount);
        verify(sales, times(0)).save(any(FlashSale.class));
    }

    @Test
    public void shouldOnlyActivateDraftSalesWithStartTimePassed() {
        final var now = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        
        // Create a draft sale ready to activate
        final var draftSale1 = new FlashSale(UUID.randomUUID(), "Draft Sale Ready", 
            now.minusHours(1), now.plusHours(1), SaleStatus.DRAFT, List.of());
        
        // Create a draft sale that shouldn't be activated (status changed externally between query and processing)
        // This tests the defensive check in the service method
        final var draftSale2 = new FlashSale(UUID.randomUUID(), "Draft Sale Changed", 
            now.minusHours(1), now.plusHours(1), SaleStatus.DRAFT, List.of());
        // Simulate status change after query but before processing
        draftSale2.setStatus(SaleStatus.ACTIVE);

        when(sales.findDraftSalesReadyToActivate(eq(SaleStatus.DRAFT), any(OffsetDateTime.class)))
            .thenReturn(List.of(draftSale1, draftSale2));

        when(sales.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final int activatedCount = service.activateDraftSales();

        assertEquals(1, activatedCount);
        assertEquals(SaleStatus.ACTIVE, draftSale1.getStatus());
        assertEquals(SaleStatus.ACTIVE, draftSale2.getStatus()); // Already active, defensive check prevented re-activation
        verify(sales, times(1)).save(draftSale1);
    }

}
