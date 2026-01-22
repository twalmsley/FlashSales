package uk.co.aosd.flash.controllers;

// Static imports for the fluent API (crucial for readability)
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.co.aosd.flash.dto.ClientProductDto;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;
import uk.co.aosd.flash.services.ProductsService;

/**
 * Admin API Web Test.
 */
@WebMvcTest(ClientRestApi.class)
@Import({ErrorMapper.class, GlobalExceptionHandler.class})
public class ClientRestApiTest {

    @Autowired
    private MockMvc mockMvc;

    private static ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductsService productsService;

    @Test
    public void shouldReturnAProductSuccessfully() throws Exception {
        final String productUuid = "e0abb92f-6bc6-4076-9a84-0eda2a45361d";

        final ProductDto productDto1 = new ProductDto(productUuid, "Dummy Product 1", "Dummy product 1 description", 101,
            BigDecimal.valueOf(99.99), 0);

        Mockito.when(productsService.getProductById(productUuid)).thenReturn(Optional.of(productDto1));

        final var getResult = mockMvc.perform(get("/api/v1/clients/products/" + productUuid)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        final var clientProduct = objectMapper.readValue(getResult.getResponse().getContentAsString(), ClientProductDto.class);
        assertEquals(productDto1.id(), clientProduct.id());
        assertEquals(productDto1.name(), clientProduct.name());
        assertEquals(productDto1.description(), clientProduct.description());
        assertEquals(productDto1.basePrice(), clientProduct.basePrice());
    }

    @Test
    public void shouldFailToFindProduct() throws Exception {
        final String productUuid = "e0abb92f-6bc6-4076-9a84-0eda2a45361d";

        Mockito.when(productsService.getProductById(productUuid)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/clients/products/" + productUuid)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andReturn();
    }

}
