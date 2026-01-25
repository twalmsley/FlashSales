package uk.co.aosd.flash.controllers;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.co.aosd.flash.config.TestSecurityConfig;
import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;
import uk.co.aosd.flash.exc.ProductNotFoundException;
import uk.co.aosd.flash.services.JwtTokenProvider;
import uk.co.aosd.flash.services.ProductsService;

@WebMvcTest(controllers = ProductRestApi.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ ErrorMapper.class, GlobalExceptionHandler.class, TestSecurityConfig.class })
@ActiveProfiles({"test", "admin-service"})
@Testcontainers
public class ProductRestApiDeleteProductByIdTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private ProductsService productsService;

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(productsService);
    }

    @Test
    public void shouldDeleteProductSuccessfully() throws Exception {
        String uuid = "146a8892-422b-4eff-a201-509bce782cb9";

        mockMvc.perform(
            delete("/api/v1/products/" + uuid))
            .andExpect(status().isOk());

        verify(productsService, times(1)).deleteProduct(uuid);
    }

    @Test
    public void shouldNotFindProductById() throws Exception {
        String uuid = "146a8892-422b-4eff-a201-509bce782cb9";

        Mockito.doThrow(new ProductNotFoundException(uuid)).when(productsService).deleteProduct(uuid);

        mockMvc.perform(
            delete("/api/v1/products/" + uuid))
            .andExpect(status().isNotFound());

        verify(productsService, times(1)).deleteProduct(uuid);
    }
}
