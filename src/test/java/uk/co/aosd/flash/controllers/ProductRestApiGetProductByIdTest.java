package uk.co.aosd.flash.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;
import uk.co.aosd.flash.services.ProductsService;

@WebMvcTest(ProductRestApi.class)
@Import({ ErrorMapper.class, GlobalExceptionHandler.class })
@Testcontainers
public class ProductRestApiGetProductByIdTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductsService productsService;

    private static ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(productsService);
    }

    @Test
    public void shouldReturnProductSuccessfully() throws Exception {
        String uuid = "146a8892-422b-4eff-a201-509bce782cb9";
        final ProductDto productDto1 = new ProductDto(uuid, "Dummy Product 1", "Dummy product 1 description", 101,
            BigDecimal.valueOf(99.99));

        Mockito.when(productsService.getProductById(uuid)).thenReturn(Optional.of(productDto1));

        final MvcResult result = mockMvc.perform(
            get("/api/v1/products/" + uuid))
            .andExpect(status().isOk())
            .andReturn();

        final ProductDto product = objectMapper.readValue(result.getResponse().getContentAsString(), ProductDto.class);
        assertNotNull(product);
        assertEquals(productDto1, product);

        verify(productsService, times(1)).getProductById(uuid);
    }

    @Test
    public void shouldNotFindProductById() throws Exception {
        String uuid = "146a8892-422b-4eff-a201-509bce782cb9";

        Mockito.when(productsService.getProductById(uuid)).thenReturn(Optional.empty());

        mockMvc.perform(
            get("/api/v1/products/" + uuid))
            .andExpect(status().isNotFound());

        verify(productsService, times(1)).getProductById(uuid);
    }
}
