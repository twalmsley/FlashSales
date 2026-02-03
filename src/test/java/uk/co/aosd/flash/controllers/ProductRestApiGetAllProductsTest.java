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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.co.aosd.flash.config.TestSecurityConfig;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;
import uk.co.aosd.flash.services.JwtTokenProvider;
import uk.co.aosd.flash.services.ProductsService;

/**
 * A Slice Test for getting all products via the Products REST API.
 */
@WebMvcTest(controllers = ProductRestApi.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ ErrorMapper.class, GlobalExceptionHandler.class, TestSecurityConfig.class })
@ActiveProfiles({"test", "admin-service"})
@Testcontainers
public class ProductRestApiGetAllProductsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

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
    public void shouldReturnProductListSuccessfully() throws Exception {
        final ProductDto productDto1 = new ProductDto("146a8892-422b-4eff-a201-509bce782cb9", "Dummy Product 1", "Dummy product 1 description", 101,
            BigDecimal.valueOf(99.99), 0);
        final ProductDto productDto2 = new ProductDto("246a8892-422b-4eff-a201-509bce782cb9", "Dummy Product 2", "Dummy product 2 description", 102,
            BigDecimal.valueOf(100.99), 0);
        final ProductDto productDto3 = new ProductDto("346a8892-422b-4eff-a201-509bce782cb9", "Dummy Product 3", "Dummy product 3 description", 103,
            BigDecimal.valueOf(101.99), 0);
        final List<ProductDto> testProducts = List.of(productDto1, productDto2, productDto3);

        Mockito.when(productsService.getAllProducts(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(testProducts);

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

        verify(productsService, times(1)).getAllProducts(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void shouldReturnFilteredProductsWithSearch() throws Exception {
        final ProductDto productDto = new ProductDto("id1", "Widget", "A useful widget", 10, BigDecimal.ONE, 0);
        Mockito.when(productsService.getAllProducts(Mockito.eq("widget"), Mockito.any(), Mockito.any())).thenReturn(List.of(productDto));

        mockMvc.perform(get("/api/v1/products").param("search", "widget"))
            .andExpect(status().isOk());

        verify(productsService, times(1)).getAllProducts(Mockito.eq("widget"), Mockito.any(), Mockito.any());
    }

    @Test
    public void shouldReturnFilteredProductsWithPriceRange() throws Exception {
        final List<ProductDto> filtered = List.of(
            new ProductDto("id1", "P1", "D1", 5, BigDecimal.valueOf(5.00), 0));
        Mockito.when(productsService.getAllProducts(Mockito.any(), Mockito.eq(BigDecimal.ONE), Mockito.eq(BigDecimal.TEN))).thenReturn(filtered);

        mockMvc.perform(get("/api/v1/products")
            .param("minPrice", "1")
            .param("maxPrice", "10"))
            .andExpect(status().isOk());

        verify(productsService, times(1)).getAllProducts(Mockito.any(), Mockito.eq(BigDecimal.ONE), Mockito.eq(BigDecimal.TEN));
    }

    @Test
    public void shouldReturn400WhenMinPriceGreaterThanMaxPrice() throws Exception {
        mockMvc.perform(get("/api/v1/products")
            .param("minPrice", "100")
            .param("maxPrice", "10"))
            .andExpect(status().isBadRequest());

        verify(productsService, times(0)).getAllProducts(Mockito.any(), Mockito.any(), Mockito.any());
    }
}
