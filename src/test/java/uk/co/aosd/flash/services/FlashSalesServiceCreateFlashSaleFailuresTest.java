package uk.co.aosd.flash.services;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;
import uk.co.aosd.flash.domain.FlashSale;
import uk.co.aosd.flash.domain.Product;
import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.CreateSaleDto;
import uk.co.aosd.flash.dto.SaleProductDto;
import uk.co.aosd.flash.exc.*;
import uk.co.aosd.flash.repository.FlashSaleItemRepository;
import uk.co.aosd.flash.repository.FlashSaleRepository;
import uk.co.aosd.flash.repository.ProductRepository;

/**
 * Tests for the FlashSalesService.
 */
public class FlashSalesServiceCreateFlashSaleFailuresTest {

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
    public void shouldFailWhenStartTimeAfterEndTime() {
        final String productUuid1 = "d2bed96f-8ed2-4c41-b495-add9994b1900";
        final String productUuid2 = "9c072608-ae3a-4b96-b24b-af9a065d0c20";
        final String productUuid3 = "d8d7182a-2df9-43b1-a3c6-d7334895e046";
        final SaleProductDto saleProduct1 = new SaleProductDto(productUuid1, 10);
        final SaleProductDto saleProduct2 = new SaleProductDto(productUuid2, 11);
        final SaleProductDto saleProduct3 = new SaleProductDto(productUuid3, 12);

        final BigDecimal basePrice1 = BigDecimal.valueOf(100.0);
        final BigDecimal basePrice2 = BigDecimal.valueOf(100.0);
        final BigDecimal basePrice3 = BigDecimal.valueOf(100.0);
        final String description1 = "Description1";
        final String description2 = "Description2";
        final String description3 = "Description3";
        final String name1 = "Product1";
        final String name2 = "Product2";
        final String name3 = "Product3";
        final int reservedCount1 = 10;
        final int reservedCount2 = 11;
        final int reservedCount3 = 12;
        final int totalPhysicalStock1 = 100;
        final int totalPhysicalStock2 = 101;
        final int totalPhysicalStock3 = 102;

        final Optional<Product> product1 = Optional
            .of(new Product(UUID.fromString(productUuid1), name1, description1, totalPhysicalStock1, basePrice1, reservedCount1));
        final Optional<Product> product2 = Optional
            .of(new Product(UUID.fromString(productUuid2), name2, description2, totalPhysicalStock2, basePrice2, reservedCount2));
        final Optional<Product> product3 = Optional
            .of(new Product(UUID.fromString(productUuid3), name3, description3, totalPhysicalStock3, basePrice3, reservedCount3));
        Mockito.when(products.findById(UUID.fromString(productUuid1))).thenReturn(product1);
        Mockito.when(products.findById(UUID.fromString(productUuid2))).thenReturn(product2);
        Mockito.when(products.findById(UUID.fromString(productUuid3))).thenReturn(product3);

        final String title = "Title";
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 01, 01, 13, 0, 0, 1, ZoneOffset.UTC);
        final OffsetDateTime endTime = OffsetDateTime.of(2026, 01, 01, 13, 0, 0, 0, ZoneOffset.UTC);
        final SaleStatus status = SaleStatus.DRAFT;

        final List<SaleProductDto> productsList = List.of(saleProduct1, saleProduct2, saleProduct3);
        final CreateSaleDto sale = new CreateSaleDto(null, title, startTime, endTime, status, productsList);

        assertThrows(InvalidSaleTimesException.class, () -> {
            service.createFlashSale(sale);
        });

    }

    @Test
    public void shouldFailWhenProductsNotFound() {
        final String productUuid1 = "d2bed96f-8ed2-4c41-b495-add9994b1900";
        final String productUuid2 = "9c072608-ae3a-4b96-b24b-af9a065d0c20";
        final String productUuid3 = "d8d7182a-2df9-43b1-a3c6-d7334895e046";
        final SaleProductDto saleProduct1 = new SaleProductDto(productUuid1, 10);
        final SaleProductDto saleProduct2 = new SaleProductDto(productUuid2, 11);
        final SaleProductDto saleProduct3 = new SaleProductDto(productUuid3, 12);

        final String title = "Title";
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 01, 01, 13, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = OffsetDateTime.of(2026, 01, 01, 14, 0, 0, 0, ZoneOffset.UTC);
        final SaleStatus status = SaleStatus.DRAFT;

        final List<SaleProductDto> productsList = List.of(saleProduct1, saleProduct2, saleProduct3);
        final CreateSaleDto sale = new CreateSaleDto(null, title, startTime, endTime, status, productsList);

        assertThrows(ProductNotFoundException.class, () -> {
            service.createFlashSale(sale);
        });

    }

    @Test
    public void shouldFailWhenDuplicateFlashSale() {
        final String productUuid1 = "d2bed96f-8ed2-4c41-b495-add9994b1900";
        final String productUuid2 = "9c072608-ae3a-4b96-b24b-af9a065d0c20";
        final String productUuid3 = "d8d7182a-2df9-43b1-a3c6-d7334895e046";
        final SaleProductDto saleProduct1 = new SaleProductDto(productUuid1, 10);
        final SaleProductDto saleProduct2 = new SaleProductDto(productUuid2, 11);
        final SaleProductDto saleProduct3 = new SaleProductDto(productUuid3, 12);

        final BigDecimal basePrice1 = BigDecimal.valueOf(100.0);
        final BigDecimal basePrice2 = BigDecimal.valueOf(100.0);
        final BigDecimal basePrice3 = BigDecimal.valueOf(100.0);
        final String description1 = "Description1";
        final String description2 = "Description2";
        final String description3 = "Description3";
        final String name1 = "Product1";
        final String name2 = "Product2";
        final String name3 = "Product3";
        final int reservedCount1 = 10;
        final int reservedCount2 = 11;
        final int reservedCount3 = 12;
        final int totalPhysicalStock1 = 100;
        final int totalPhysicalStock2 = 101;
        final int totalPhysicalStock3 = 102;

        final Optional<Product> product1 = Optional
            .of(new Product(UUID.fromString(productUuid1), name1, description1, totalPhysicalStock1, basePrice1, reservedCount1));
        final Optional<Product> product2 = Optional
            .of(new Product(UUID.fromString(productUuid2), name2, description2, totalPhysicalStock2, basePrice2, reservedCount2));
        final Optional<Product> product3 = Optional
            .of(new Product(UUID.fromString(productUuid3), name3, description3, totalPhysicalStock3, basePrice3, reservedCount3));
        Mockito.when(products.findById(UUID.fromString(productUuid1))).thenReturn(product1);
        Mockito.when(products.findById(UUID.fromString(productUuid2))).thenReturn(product2);
        Mockito.when(products.findById(UUID.fromString(productUuid3))).thenReturn(product3);

        final String title = "Title";
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 01, 01, 13, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = OffsetDateTime.of(2026, 01, 01, 14, 0, 0, 0, ZoneOffset.UTC);
        final SaleStatus status = SaleStatus.DRAFT;
        final FlashSale newSale = new FlashSale(null, title, startTime, endTime, status, List.of());

        Mockito.when(sales.save(newSale)).thenThrow(DuplicateKeyException.class);

        final List<SaleProductDto> productsList = List.of(saleProduct1, saleProduct2, saleProduct3);
        final CreateSaleDto sale = new CreateSaleDto(null, title, startTime, endTime, status, productsList);

        assertThrows(DuplicateEntityException.class, () -> {
            service.createFlashSale(sale);
        });

    }

    @Test
    public void shouldFailWhenSaleDurationTooShort() {
        final String productUuid1 = "d2bed96f-8ed2-4c41-b495-add9994b1900";
        final String productUuid2 = "9c072608-ae3a-4b96-b24b-af9a065d0c20";
        final String productUuid3 = "d8d7182a-2df9-43b1-a3c6-d7334895e046";
        final SaleProductDto saleProduct1 = new SaleProductDto(productUuid1, 10);
        final SaleProductDto saleProduct2 = new SaleProductDto(productUuid2, 11);
        final SaleProductDto saleProduct3 = new SaleProductDto(productUuid3, 12);

        final BigDecimal basePrice1 = BigDecimal.valueOf(100.0);
        final BigDecimal basePrice2 = BigDecimal.valueOf(100.0);
        final BigDecimal basePrice3 = BigDecimal.valueOf(100.0);
        final String description1 = "Description1";
        final String description2 = "Description2";
        final String description3 = "Description3";
        final String name1 = "Product1";
        final String name2 = "Product2";
        final String name3 = "Product3";
        final int reservedCount1 = 10;
        final int reservedCount2 = 11;
        final int reservedCount3 = 12;
        final int totalPhysicalStock1 = 100;
        final int totalPhysicalStock2 = 101;
        final int totalPhysicalStock3 = 102;

        final Optional<Product> product1 = Optional
            .of(new Product(UUID.fromString(productUuid1), name1, description1, totalPhysicalStock1, basePrice1, reservedCount1));
        final Optional<Product> product2 = Optional
            .of(new Product(UUID.fromString(productUuid2), name2, description2, totalPhysicalStock2, basePrice2, reservedCount2));
        final Optional<Product> product3 = Optional
            .of(new Product(UUID.fromString(productUuid3), name3, description3, totalPhysicalStock3, basePrice3, reservedCount3));
        Mockito.when(products.findById(UUID.fromString(productUuid1))).thenReturn(product1);
        Mockito.when(products.findById(UUID.fromString(productUuid2))).thenReturn(product2);
        Mockito.when(products.findById(UUID.fromString(productUuid3))).thenReturn(product3);

        final String title = "Title";
        //
        // NOTE: Nanos are too fine grained and don't get picked up - not a problem for
        // this use case.
        //
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 01, 01, 13, 0, 1, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = OffsetDateTime.of(2026, 01, 01, 13, 10, 0, 0, ZoneOffset.UTC);
        final SaleStatus status = SaleStatus.DRAFT;

        final List<SaleProductDto> productsList = List.of(saleProduct1, saleProduct2, saleProduct3);
        final CreateSaleDto sale = new CreateSaleDto(null, title, startTime, endTime, status, productsList);

        assertThrows(SaleDurationTooShortException.class, () -> {
            service.createFlashSale(sale);
        });

    }

    @Test
    public void shouldFailWhenProductIdIsNotAUUID() {
        final String productUuid1 = "invalid uuid";
        final SaleProductDto saleProduct1 = new SaleProductDto(productUuid1, 10);

        //
        // NOTE: Nanos are too fine grained and don't get picked up - not a problem for
        // this use case.
        //
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 01, 01, 13, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = OffsetDateTime.of(2026, 01, 01, 14, 0, 0, 0, ZoneOffset.UTC);
        final SaleStatus status = SaleStatus.DRAFT;

        final List<SaleProductDto> productsList = List.of(saleProduct1);
        final CreateSaleDto sale = new CreateSaleDto(null, "Title", startTime, endTime, status, productsList);

        assertThrows(ProductNotFoundException.class, () -> {
            service.createFlashSale(sale);
        });

    }

    @Test
    public void shouldFailDueToInsufficientStock() {
        final String productUuid1 = "d2bed96f-8ed2-4c41-b495-add9994b1900";
        final SaleProductDto saleProduct1 = new SaleProductDto(productUuid1, 10);

        final BigDecimal basePrice1 = BigDecimal.valueOf(100.0);
        final String description1 = "Description1";
        final String name1 = "Product1";
        final int reservedCount1 = 100;
        final int totalPhysicalStock1 = 100;

        final Optional<Product> product1 = Optional
            .of(new Product(UUID.fromString(productUuid1), name1, description1, totalPhysicalStock1, basePrice1, reservedCount1));
        Mockito.when(products.findById(UUID.fromString(productUuid1))).thenReturn(product1);

        final String title = "Title";
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 01, 01, 12, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = OffsetDateTime.of(2026, 01, 01, 12, 10, 0, 0, ZoneOffset.UTC);
        final SaleStatus status = SaleStatus.DRAFT;
        final FlashSale newSale = new FlashSale(null, title, startTime, endTime, status, List.of());
        final FlashSale savedSale = new FlashSale(UUID.fromString("db22fee4-35d7-4b66-82b5-6e9f3c3643ea"), title, startTime, endTime, status, List.of());

        Mockito.when(sales.save(newSale)).thenReturn(savedSale);

        final List<SaleProductDto> productsList = List.of(saleProduct1);
        final CreateSaleDto sale = new CreateSaleDto(null, title, startTime, endTime, status, productsList);

        assertThrows(InsufficientResourcesException.class, () -> {
            service.createFlashSale(sale);
        });
    }
}
