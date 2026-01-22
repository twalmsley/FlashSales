package uk.co.aosd.flash.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;
import uk.co.aosd.flash.exc.ProductNotFoundException;
import uk.co.aosd.flash.services.ProductsService;

/**
 * Test REST API for updating a product.
 */
@WebMvcTest(ProductRestApi.class)
@Import({ ErrorMapper.class, GlobalExceptionHandler.class, })
@Testcontainers
public class ProductRestApiUpdateProductTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductsService productsService;

    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void beforeAll() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(productsService);
    }

    @Test
    public void shouldUpdateAProductSuccessfully() throws Exception {
        final String uuid = "846a8892-422b-4eff-a201-509bce782cb9";
        final ProductDto productDto = new ProductDto(uuid, "Dummy Product 1", "Dummy product 1 description", 101,
            BigDecimal.valueOf(99.99), 0);

        mockMvc.perform(
            put("/api/v1/products/" + uuid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDto)))
            .andExpect(status().isOk());

        verify(productsService, times(1)).updateProduct(uuid, productDto);
    }

    @Test
    public void shouldRejectAnInvalidProductBeanOnUpdate() throws Exception {
        final ProductDto productDto = new ProductDto("uuid", "", "", -101,
            BigDecimal.valueOf(-99.99), -1);

        mockMvc.perform(
            put("/api/v1/products/uuid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDto)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(content().string(containsString("basePrice: Price cannot be negative")))
            .andExpect(content().string(containsString("name: A name must be provided.")))
            .andExpect(content().string(containsString("description: A description must be provided.")))
            .andExpect(content().string(containsString("totalPhysicalStock: Stock cannot be negative")))
            .andExpect(content().string(containsString("reservedCount: ReservedCount cannot be negative")));

        verify(productsService, times(0)).updateProduct("uuid", productDto);

    }

    @Test
    public void shouldNotFindInvalidProductIdOnUpdate() throws Exception {
        final String uuid = "846a8892-422b-4eff-a201-509bce782cb9";
        final String name = "Dummy Product 1";
        final ProductDto productDto = new ProductDto(uuid, name, "Dummy product 1 description", 101,
            BigDecimal.valueOf(99.99), 0);

        Mockito.doThrow(new ProductNotFoundException(uuid)).when(productsService).updateProduct(uuid, productDto);

        mockMvc.perform(
            put("/api/v1/products/" + uuid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDto)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value(containsString(uuid)));

        verify(productsService, times(1)).updateProduct(uuid, productDto);
    }
}
