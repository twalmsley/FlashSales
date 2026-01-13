package uk.co.aosd.flash.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;
import uk.co.aosd.flash.services.ProductsService;

@WebMvcTest(ProductRestApi.class)
@Import({ ErrorMapper.class, GlobalExceptionHandler.class })
public class ProductRestApiGetAllProductsTest {

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
    public void shouldReturnProductListSuccessfully() throws Exception {
        final String uuid1 = "146a8892-422b-4eff-a201-509bce782cb9";
        final String uuid2 = "246a8892-422b-4eff-a201-509bce782cb9";
        final String uuid3 = "346a8892-422b-4eff-a201-509bce782cb9";
        final ProductDto productDto1 = new ProductDto(uuid1, "Dummy Product 1", "Dummy product 1 description", 101, BigDecimal.valueOf(99.99));
        final ProductDto productDto2 = new ProductDto(uuid2, "Dummy Product 2", "Dummy product 2 description", 102, BigDecimal.valueOf(100.99));
        final ProductDto productDto3 = new ProductDto(uuid3, "Dummy Product 3", "Dummy product 3 description", 103, BigDecimal.valueOf(101.99));
        final List<ProductDto> testProducts = List.of(productDto1, productDto2, productDto3);

        Mockito.when(productsService.getAllProducts()).thenReturn(testProducts);

        final MvcResult result = mockMvc.perform(
            get("/api/v1/products"))
            .andExpect(status().isOk())
            .andReturn();

        final ProductDto[] products = objectMapper.readValue(result.getResponse().getContentAsString(), ProductDto[].class);
        assertNotNull(products);
        assertEquals(3, products.length);

        Arrays.asList(products).forEach(p -> {
            assertTrue(testProducts.contains(p));
        });

        verify(productsService, times(1)).getAllProducts();
    }
}
