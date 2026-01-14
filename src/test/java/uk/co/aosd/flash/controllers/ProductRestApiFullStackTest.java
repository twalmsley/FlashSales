package uk.co.aosd.flash.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.co.aosd.flash.dto.ProductDto;

@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
public class ProductRestApiFullStackTest {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres");

    @Autowired
    private MockMvc mockMvc;

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void shouldCreateAProductSuccessfully() throws Exception {
        final ProductDto productDto1 = new ProductDto(null, "Dummy Product 1", "Dummy product 1 description", 101,
            BigDecimal.valueOf(99.99));

        final var response = mockMvc.perform(
            post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDto1)))
            .andExpect(status().isCreated())
            .andReturn();

        final String uri = response.getResponse().getHeader(HttpHeaders.LOCATION);

        final var result = mockMvc.perform(
            get(uri))
            .andReturn();

        final ProductDto product = objectMapper.readValue(result.getResponse().getContentAsString(), ProductDto.class);
        assertNotNull(product);
        assertEquals(productDto1.name(), product.name());
        assertEquals(productDto1.description(), product.description());
        assertEquals(productDto1.totalPhysicalStock(), product.totalPhysicalStock());
        assertEquals(productDto1.basePrice(), product.basePrice());
    }
}
