package uk.co.aosd.flash.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.annotation.EnableCaching;
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
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.dto.ProductStockDto;
import uk.co.aosd.flash.dto.UpdateProductStockDto;

/**
 * Full stack test for the products REST API.
 */
@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
@EnableCaching
@ActiveProfiles({"test", "admin-service", "api-service"})
public class ProductRestApiFullStackTest {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres");

    @Container
    @ServiceConnection(name = "redis")
    @SuppressWarnings("resource")
    public static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void beforeAll() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor withAdminUser() {
        final UUID adminUserId = UUID.randomUUID();
        return SecurityMockMvcRequestPostProcessors.authentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                adminUserId, null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN_USER"))));
    }

    /**
     * Test CRUD operations for a Product.
     */
    @Test
    public void shouldCreateAProductUpdateAndRetrieveItSuccessfully() throws Exception {
        //
        // CREATE
        //
        final ProductDto productDto1 = new ProductDto(null, "Dummy Product 1", "Dummy product 1 description", 101,
            BigDecimal.valueOf(99.99), 0);

        final var response = mockMvc.perform(
            post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDto1)))
            .andExpect(status().isCreated())
            .andReturn();

        //
        // GET and verify
        //
        final String uri = response.getResponse().getHeader(HttpHeaders.LOCATION);

        final var result = mockMvc.perform(
            get(uri)
                .with(withAdminUser()))
            .andReturn();

        final ProductDto product = objectMapper.readValue(result.getResponse().getContentAsString(), ProductDto.class);
        assertNotNull(product);
        assertEquals(productDto1.name(), product.name());
        assertEquals(productDto1.description(), product.description());
        assertEquals(productDto1.totalPhysicalStock(), product.totalPhysicalStock());
        assertEquals(productDto1.basePrice(), product.basePrice());

        //
        // UPDATE
        //
        final ProductDto updatedProductDto = new ProductDto(product.id(), "Dummy Product 1 - updated", "Dummy product 1 description - updated", 201,
            BigDecimal.valueOf(199.99), 0);

        mockMvc.perform(
            put(uri)
                .with(withAdminUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedProductDto)))
            .andExpect(status().isOk());

        //
        // GET and verify
        //
        final var result2 = mockMvc.perform(
            get(uri)
                .with(withAdminUser()))
            .andReturn();

        final ProductDto productDto2 = objectMapper.readValue(result2.getResponse().getContentAsString(), ProductDto.class);
        assertNotNull(productDto2);
        assertEquals(productDto2.id(), updatedProductDto.id());
        assertEquals(productDto2.name(), updatedProductDto.name());
        assertEquals(productDto2.description(), updatedProductDto.description());
        assertEquals(productDto2.totalPhysicalStock(), updatedProductDto.totalPhysicalStock());
        assertEquals(productDto2.basePrice(), updatedProductDto.basePrice());
        assertEquals(productDto2.reservedCount(), updatedProductDto.reservedCount());

        //
        // DELETE
        //
        mockMvc.perform(
            delete(uri)
                .with(withAdminUser()))
            .andExpect(status().isOk());

        //
        // GET and verify
        //
        mockMvc.perform(
            get(uri)
                .with(withAdminUser()))
            .andExpect(status().isNotFound());
    }

    /**
     * Test that reservedCount > totalPhysicalStock causes and Exception.
     */
    @Test
    public void shouldRejectInvalidReservedCount() throws Exception {
        //
        // CREATE
        //
        final ProductDto productDto1 = new ProductDto(null, "Dummy Product 1", "Dummy product 1 description", 101,
            BigDecimal.valueOf(99.99), 102);

        mockMvc.perform(
            post("/api/v1/products")
                .with(withAdminUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDto1)))
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void shouldGetAndUpdateProductStockSuccessfully() throws Exception {
        //
        // CREATE
        //
        final ProductDto productDto1 = new ProductDto(null, "Stock Product 1", "Stock product 1 description", 100,
            BigDecimal.valueOf(9.99), 10);

        final var response = mockMvc.perform(
            post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDto1)))
            .andExpect(status().isCreated())
            .andReturn();

        final String uri = response.getResponse().getHeader(HttpHeaders.LOCATION);
        final String stockUri = uri + "/stock";

        //
        // GET stock and verify
        //
        final var stockResult = mockMvc.perform(get(stockUri).with(withAdminUser())).andExpect(status().isOk()).andReturn();
        final ProductStockDto stock = objectMapper.readValue(stockResult.getResponse().getContentAsString(), ProductStockDto.class);
        assertNotNull(stock);
        assertEquals(100, stock.totalPhysicalStock());
        assertEquals(10, stock.reservedCount());
        assertEquals(90, stock.availableStock());

        //
        // UPDATE stock (increase)
        //
        final UpdateProductStockDto updateStock = new UpdateProductStockDto(120);
        final var updateResult = mockMvc.perform(
            put(stockUri)
                .with(withAdminUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateStock)))
            .andExpect(status().isOk())
            .andReturn();

        final ProductStockDto updatedStock = objectMapper.readValue(updateResult.getResponse().getContentAsString(), ProductStockDto.class);
        assertNotNull(updatedStock);
        assertEquals(120, updatedStock.totalPhysicalStock());
        assertEquals(10, updatedStock.reservedCount());
        assertEquals(110, updatedStock.availableStock());

        //
        // UPDATE stock (below reserved) should fail
        //
        final UpdateProductStockDto invalidUpdate = new UpdateProductStockDto(5);
        mockMvc.perform(
            put(stockUri)
                .with(withAdminUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUpdate)))
            .andExpect(status().isBadRequest());
    }
}
