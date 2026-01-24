package uk.co.aosd.flash.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.co.aosd.flash.dto.ProductStockDto;
import uk.co.aosd.flash.dto.UpdateProductStockDto;
import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;
import uk.co.aosd.flash.exc.ProductNotFoundException;
import uk.co.aosd.flash.services.ProductsService;

/**
 * Slice tests for product stock endpoints.
 */
@WebMvcTest(ProductRestApi.class)
@Import({ ErrorMapper.class, GlobalExceptionHandler.class })
@Testcontainers
public class ProductRestApiStockTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductsService productsService;

    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void beforeAll() {
        objectMapper = new ObjectMapper();
    }

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(productsService);
    }

    @Test
    public void shouldReturnProductStockSuccessfully() throws Exception {
        final String uuid = "846a8892-422b-4eff-a201-509bce782cb9";
        final ProductStockDto stock = new ProductStockDto(uuid, 100, 10, 90);
        Mockito.when(productsService.getProductStockById(uuid)).thenReturn(stock);

        mockMvc.perform(get("/api/v1/products/" + uuid + "/stock"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(uuid))
            .andExpect(jsonPath("$.totalPhysicalStock").value(100))
            .andExpect(jsonPath("$.reservedCount").value(10))
            .andExpect(jsonPath("$.availableStock").value(90));

        verify(productsService, times(1)).getProductStockById(uuid);
    }

    @Test
    public void shouldReturn404WhenProductNotFoundForStock() throws Exception {
        final String uuid = "846a8892-422b-4eff-a201-509bce782cb9";
        Mockito.doThrow(new ProductNotFoundException(uuid)).when(productsService).getProductStockById(uuid);

        mockMvc.perform(get("/api/v1/products/" + uuid + "/stock"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value(containsString(uuid)));

        verify(productsService, times(1)).getProductStockById(uuid);
    }

    @Test
    public void shouldUpdateProductStockSuccessfully() throws Exception {
        final String uuid = "846a8892-422b-4eff-a201-509bce782cb9";
        final UpdateProductStockDto updateDto = new UpdateProductStockDto(250);
        final ProductStockDto updated = new ProductStockDto(uuid, 250, 10, 240);
        Mockito.when(productsService.updateProductStock(uuid, updateDto)).thenReturn(updated);

        mockMvc.perform(
            put("/api/v1/products/" + uuid + "/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(uuid))
            .andExpect(jsonPath("$.totalPhysicalStock").value(250))
            .andExpect(jsonPath("$.reservedCount").value(10))
            .andExpect(jsonPath("$.availableStock").value(240));

        verify(productsService, times(1)).updateProductStock(uuid, updateDto);
    }

    @Test
    public void shouldRejectUpdatingStockBelowReservedCount() throws Exception {
        final String uuid = "846a8892-422b-4eff-a201-509bce782cb9";
        final UpdateProductStockDto updateDto = new UpdateProductStockDto(5);
        Mockito.doThrow(new IllegalArgumentException("Total physical stock (5) cannot be less than reserved count (10)"))
            .when(productsService).updateProductStock(uuid, updateDto);

        mockMvc.perform(
            put("/api/v1/products/" + uuid + "/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("Total physical stock")));

        verify(productsService, times(1)).updateProductStock(uuid, updateDto);
    }
}

