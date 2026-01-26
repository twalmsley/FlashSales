package uk.co.aosd.flash.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import uk.co.aosd.flash.config.TestSecurityConfig;
import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.CreateSaleDto;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.dto.SaleProductDto;
import uk.co.aosd.flash.repository.FlashSaleItemRepository;
import uk.co.aosd.flash.repository.FlashSaleRepository;
import uk.co.aosd.flash.repository.ProductRepository;

/**
 * Full stack test for the products REST API.
 */
@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
@Import(TestSecurityConfig.class)
@EnableCaching
@ActiveProfiles({"test", "admin-service", "api-service"})
public class FlashSaleRestApiFullStackTest {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres");

    @Container
    @ServiceConnection(name = "redis")
    @SuppressWarnings("resource")
    public static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FlashSaleRepository salesRepo;

    @Autowired
    private FlashSaleItemRepository itemsRepo;

    @Autowired
    private ProductRepository productsRepo;

    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void beforeAll() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    public void beforeEach() {
        // Ensure tests are isolated even when the Spring context / containers are
        // reused.
        itemsRepo.deleteAll();
        salesRepo.deleteAll();
        productsRepo.deleteAll();
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor withAdminUser() {
        final UUID adminUserId = UUID.randomUUID();
        return SecurityMockMvcRequestPostProcessors.authentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                adminUserId, null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN_USER"))));
    }

    /**
     * Test create flash sale successfully.
     */
    @Test
    public void shouldCreateAFlashSaleSuccessfully() throws Exception {
        //
        // CREATE a product
        //
        final ProductDto productDto1 = new ProductDto(null, "Dummy Product 1", "Dummy product 1 description", 101,
            BigDecimal.valueOf(99.99), 10);

        final var response = mockMvc.perform(
            post("/api/v1/products")
                .with(withAdminUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDto1)))
            .andExpect(status().isCreated())
            .andReturn();

        //
        // GET the product
        //
        final String productUri = response.getResponse().getHeader(HttpHeaders.LOCATION);

        final var result = mockMvc.perform(
            get(productUri)
                .with(withAdminUser()))
            .andReturn();
        final ProductDto product = objectMapper.readValue(result.getResponse().getContentAsString(), ProductDto.class);

        //
        // Create a flash sale for the product
        //
        final SaleProductDto saleProduct = new SaleProductDto(product.id(), 10);
        final CreateSaleDto sale = new CreateSaleDto(null, "Test Sale", OffsetDateTime.of(2026, 01, 01, 12, 0, 0, 0, ZoneOffset.UTC),
            OffsetDateTime.of(2026, 01, 01, 13, 0, 0, 0, ZoneOffset.UTC), SaleStatus.DRAFT, List.of(saleProduct));

        final var createFlashSaleResult = mockMvc.perform(
            post("/api/v1/admin/flash_sale")
                .with(withAdminUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sale)))
            .andExpect(status().isCreated())
            .andReturn();

        //
        // Verify the results in the database.
        //
        final String uri = createFlashSaleResult.getResponse().getHeader(HttpHeaders.LOCATION);
        final String uuidString = uri.substring("/api/v1/admin/flash_sale/".length());
        final var saleUuid = UUID.fromString(uuidString);
        final var maybeSavedSale = salesRepo.findById(saleUuid);
        assertTrue(maybeSavedSale.isPresent());
        maybeSavedSale.ifPresent(savedSale -> {
            assertEquals(sale.title(), savedSale.getTitle());
            assertEquals(sale.startTime(), savedSale.getStartTime());
            assertEquals(sale.endTime(), savedSale.getEndTime());
            assertEquals(sale.status(), savedSale.getStatus());
        });
        //
        // Check the products and sale items are correct.
        //
        final var allItems = itemsRepo.findAll();
        assertEquals(1, allItems.size());
        final var savedItem = allItems.get(0);
        assertEquals(0, savedItem.getSoldCount());
        assertEquals(BigDecimal.valueOf(99.99), savedItem.getSalePrice());
        assertEquals(10, savedItem.getAllocatedStock());
        assertEquals(product.id(), savedItem.getProduct().getId().toString());
        assertEquals(uuidString, savedItem.getFlashSale().getId().toString());

        final var maybeSavedProduct = productsRepo.findById(UUID.fromString(product.id()));
        assertTrue(maybeSavedProduct.isPresent());
        maybeSavedProduct.ifPresent(savedProduct -> {
            assertEquals(20, savedProduct.getReservedCount());
        });
    }

    @Test
    public void shouldListActiveSalesWhenFilteringByStatusOnlyWithNoDates() throws Exception {
        //
        // CREATE a product
        //
        final ProductDto productDto1 = new ProductDto(null, "Dummy Product 1", "Dummy product 1 description", 101,
            BigDecimal.valueOf(99.99), 10);

        final var response = mockMvc.perform(
            post("/api/v1/products")
                .with(withAdminUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDto1)))
            .andExpect(status().isCreated())
            .andReturn();

        //
        // GET the product
        //
        final String productUri = response.getResponse().getHeader(HttpHeaders.LOCATION);

        final var result = mockMvc.perform(get(productUri)
            .with(withAdminUser())).andReturn();
        final ProductDto product = objectMapper.readValue(result.getResponse().getContentAsString(), ProductDto.class);

        //
        // Create an ACTIVE flash sale for the product
        //
        final SaleProductDto saleProduct = new SaleProductDto(product.id(), 10);
        final CreateSaleDto sale = new CreateSaleDto(null, "Test Sale", OffsetDateTime.of(2026, 01, 01, 12, 0, 0, 0, ZoneOffset.UTC),
            OffsetDateTime.of(2026, 01, 01, 13, 0, 0, 0, ZoneOffset.UTC), SaleStatus.ACTIVE, List.of(saleProduct));

        mockMvc.perform(
            post("/api/v1/admin/flash_sale")
                .with(withAdminUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sale)))
            .andExpect(status().isCreated());

        //
        // List ACTIVE sales with no start/end date filters.
        // This previously failed against Postgres due to typeless NULL parameters.
        //
        mockMvc.perform(get("/api/v1/admin/flash_sale")
            .with(withAdminUser())
            .param("status", "ACTIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    public void shouldListOnlySalesWhoseTimePeriodOverlapsTheSpecifiedWindow() throws Exception {
        //
        // CREATE a product
        //
        final ProductDto productDto1 = new ProductDto(null, "Dummy Product 1", "Dummy product 1 description", 101,
            BigDecimal.valueOf(99.99), 10);

        final var response = mockMvc.perform(
            post("/api/v1/products")
                .with(withAdminUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDto1)))
            .andExpect(status().isCreated())
            .andReturn();

        //
        // GET the product
        //
        final String productUri = response.getResponse().getHeader(HttpHeaders.LOCATION);

        final var result = mockMvc.perform(get(productUri)
            .with(withAdminUser())).andReturn();
        final ProductDto product = objectMapper.readValue(result.getResponse().getContentAsString(), ProductDto.class);

        final SaleProductDto saleProduct = new SaleProductDto(product.id(), 10);

        // Sale A: 12:00-13:00 (overlaps window 12:30-12:45)
        final CreateSaleDto saleA = new CreateSaleDto(null, "Sale A",
            OffsetDateTime.of(2026, 01, 01, 12, 0, 0, 0, ZoneOffset.UTC),
            OffsetDateTime.of(2026, 01, 01, 13, 0, 0, 0, ZoneOffset.UTC),
            SaleStatus.ACTIVE, List.of(saleProduct));

        // Sale B: 14:00-15:00 (does NOT overlap window 12:30-12:45)
        final CreateSaleDto saleB = new CreateSaleDto(null, "Sale B",
            OffsetDateTime.of(2026, 01, 01, 14, 0, 0, 0, ZoneOffset.UTC),
            OffsetDateTime.of(2026, 01, 01, 15, 0, 0, 0, ZoneOffset.UTC),
            SaleStatus.ACTIVE, List.of(saleProduct));

        mockMvc.perform(
            post("/api/v1/admin/flash_sale")
                .with(withAdminUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saleA)))
            .andExpect(status().isCreated());

        mockMvc.perform(
            post("/api/v1/admin/flash_sale")
                .with(withAdminUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saleB)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/admin/flash_sale")
            .with(withAdminUser())
            .param("status", "ACTIVE")
            .param("startDate", "2026-01-01T12:30:00Z")
            .param("endDate", "2026-01-01T12:45:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("Sale A"))
            .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }
}
