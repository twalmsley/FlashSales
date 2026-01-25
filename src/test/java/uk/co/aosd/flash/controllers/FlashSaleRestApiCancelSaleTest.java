package uk.co.aosd.flash.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.co.aosd.flash.config.TestSecurityConfig;
import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;
import uk.co.aosd.flash.exc.FlashSaleNotFoundException;
import uk.co.aosd.flash.services.FlashSalesService;
import uk.co.aosd.flash.services.JwtTokenProvider;

/**
 * Flash Sale REST API test for cancelling a Flash Sale.
 */
@WebMvcTest(controllers = FlashSaleAdminRestApi.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ ErrorMapper.class, GlobalExceptionHandler.class, TestSecurityConfig.class })
@ActiveProfiles({"test", "admin-service"})
public class FlashSaleRestApiCancelSaleTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private FlashSalesService salesService;

    @BeforeAll
    public static void beforeAll() {
        // No setup needed
    }

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(salesService);
    }

    @Test
    public void shouldCancelFlashSaleSuccessfully() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);

        // Service method should complete without throwing
        Mockito.doNothing().when(salesService).cancelFlashSale(saleUuid);

        mockMvc.perform(
            post("/api/v1/admin/flash_sale/" + saleId + "/cancel")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        verify(salesService, times(1)).cancelFlashSale(saleUuid);
    }

    @Test
    public void shouldReturnNotFoundWhenSaleDoesNotExist() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);

        Mockito.doThrow(new FlashSaleNotFoundException(saleUuid))
            .when(salesService).cancelFlashSale(saleUuid);

        mockMvc.perform(
            post("/api/v1/admin/flash_sale/" + saleId + "/cancel")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value(containsString("Flash sale with id '" + saleId + "' not found")));

        verify(salesService, times(1)).cancelFlashSale(saleUuid);
    }

    @Test
    public void shouldReturnBadRequestWhenCancellingCompletedSale() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);

        Mockito.doThrow(new IllegalArgumentException("Cannot cancel a COMPLETED sale"))
            .when(salesService).cancelFlashSale(saleUuid);

        mockMvc.perform(
            post("/api/v1/admin/flash_sale/" + saleId + "/cancel")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Cannot cancel a COMPLETED sale"));

        verify(salesService, times(1)).cancelFlashSale(saleUuid);
    }

    @Test
    public void shouldReturnBadRequestWhenCancellingAlreadyCancelledSale() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);

        Mockito.doThrow(new IllegalArgumentException("Sale is already CANCELLED"))
            .when(salesService).cancelFlashSale(saleUuid);

        mockMvc.perform(
            post("/api/v1/admin/flash_sale/" + saleId + "/cancel")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Sale is already CANCELLED"));

        verify(salesService, times(1)).cancelFlashSale(saleUuid);
    }

    @Test
    public void shouldReturnBadRequestWhenInvalidUuidFormat() throws Exception {
        final String invalidSaleId = "invalid-uuid";

        mockMvc.perform(
            post("/api/v1/admin/flash_sale/" + invalidSaleId + "/cancel")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        // Service should not be called with invalid UUID
        verify(salesService, times(0)).cancelFlashSale(Mockito.any(UUID.class));
    }

}
