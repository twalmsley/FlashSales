package uk.co.aosd.flash.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.co.aosd.flash.dto.ProductDto;

@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
@EnableCaching
public class ProductRestApiFullStackTest {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres");

    @Container
    @ServiceConnection(name = "redis")
    public static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    private static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Test CRUD operations for a Product.
     */
    @Test
    public void shouldCreateAProductUpdateAndRetrieveItSuccessfully() throws Exception {
        //
        // CREATE
        //
        final ProductDto productDto1 = new ProductDto(null, "Dummy Product 1", "Dummy product 1 description", 101,
            BigDecimal.valueOf(99.99));

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

        final long startGet1 = System.currentTimeMillis();
        final var result = mockMvc.perform(
            get(uri))
            .andReturn();
        final long endGet1 = System.currentTimeMillis();


        final long startGet2 = System.currentTimeMillis();
        mockMvc.perform(
            get(uri))
            .andReturn();
        final long endGet2 = System.currentTimeMillis();

        //
        // Possibly dodgy assertion meant to check if the cache is working
        //
        final long duration1 = endGet1 - startGet1;
        final long duration2 = endGet2 - startGet2;
        assertTrue((duration1/duration2) > 10);

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
            BigDecimal.valueOf(199.99));

        mockMvc.perform(
            put(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedProductDto)))
            .andExpect(status().isOk());

        //
        // GET and verify
        //
        final var result2 = mockMvc.perform(
            get(uri))
            .andReturn();

        final ProductDto productDto2 = objectMapper.readValue(result2.getResponse().getContentAsString(), ProductDto.class);
        assertNotNull(productDto2);
        assertEquals(productDto2.id(), updatedProductDto.id());
        assertEquals(productDto2.name(), updatedProductDto.name());
        assertEquals(productDto2.description(), updatedProductDto.description());
        assertEquals(productDto2.totalPhysicalStock(), updatedProductDto.totalPhysicalStock());
        assertEquals(productDto2.basePrice(), updatedProductDto.basePrice());

        //
        // DELETE
        //
        mockMvc.perform(
            delete(uri))
            .andExpect(status().isOk());

        //
        // GET and verify
        //
        mockMvc.perform(
            get(uri))
            .andExpect(status().isNotFound());
    }
}
