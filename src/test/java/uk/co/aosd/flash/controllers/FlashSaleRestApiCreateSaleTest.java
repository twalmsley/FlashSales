package uk.co.aosd.flash.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.CreateSaleDto;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;
import uk.co.aosd.flash.exc.DuplicateEntityException;
import uk.co.aosd.flash.services.FlashSalesService;

@WebMvcTest(FlashSaleRestApi.class)
@Import({ ErrorMapper.class, GlobalExceptionHandler.class })
public class FlashSaleRestApiCreateSaleTest {

    @Autowired
    private MockMvc mockMvc;

    private static ObjectMapper objectMapper;

    @MockitoBean
    private FlashSalesService salesService;

    @BeforeAll
    public static void beforeAll() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(salesService);
    }

    @Test
    public void shouldCreateAFlashSaleSuccessfully() throws Exception {
        final ProductDto productDto = new ProductDto("846a8892-422b-4eff-a201-509bce782cb9", "Dummy Product 1", "Dummy product 1 description", 101,
            BigDecimal.valueOf(99.99));
        final CreateSaleDto saleDto = new CreateSaleDto(null, "Dummy Sale 1", LocalDateTime.of(2026, 01, 01, 12, 00, 00),
            LocalDateTime.of(2026, 01, 01, 13, 00, 00), SaleStatus.DRAFT, List.of(productDto));

        final String saleUuid = "e00813e5-c928-4477-ba27-dacb62781d5c";
        Mockito.when(salesService.createFlashSale(saleDto)).thenReturn(UUID.fromString(saleUuid));

        mockMvc.perform(
            post("/api/v1/admin/flash_sale")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saleDto)))
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/sales/" + saleUuid));

        verify(salesService, times(1)).createFlashSale(saleDto);
    }

    @Test
    public void shouldRejectAnInvalidSalesBeanOnCreate() throws Exception {
        final CreateSaleDto saleDto = new CreateSaleDto(null, "", null,
            null, null, List.of());

        mockMvc.perform(
            post("/api/v1/admin/flash_sale")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saleDto)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(content().string(containsString("A non-empty title is needed for the sale.")))
            .andExpect(content().string(containsString("The sale needs a valid start time.")))
            .andExpect(content().string(containsString("The sale needs a valid end time.")))
            .andExpect(content().string(containsString("The sale needs a valid status.")))
            .andExpect(content().string(containsString("The sale needs at least one product.")));

        verify(salesService, times(0)).createFlashSale(saleDto);

    }

    @Test
    public void shouldRejectAnInvalidProductBeanOnCreate() throws Exception {
        final ProductDto productDto = new ProductDto("uuid", "", "", -101,
            BigDecimal.valueOf(-99.99));
        final CreateSaleDto saleDto = new CreateSaleDto(null, "Dummy Sale 1", LocalDateTime.of(2026, 01, 01, 12, 00, 00),
            LocalDateTime.of(2026, 01, 01, 13, 00, 00), SaleStatus.DRAFT, List.of(productDto));

        mockMvc.perform(
            post("/api/v1/admin/flash_sale")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saleDto)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(content().string(containsString("basePrice: Price cannot be negative")))
            .andExpect(content().string(containsString("name: A name must be provided.")))
            .andExpect(content().string(containsString("description: A description must be provided.")))
            .andExpect(content().string(containsString("totalPhysicalStock: Stock cannot be negative")));

        verify(salesService, times(0)).createFlashSale(saleDto);

    }

    @Test
    public void shouldRejectDuplicateSaleOnCreate() throws Exception {
        final ProductDto productDto = new ProductDto("846a8892-422b-4eff-a201-509bce782cb9", "Dummy Product 1", "Dummy product 1 description", 101,
            BigDecimal.valueOf(99.99));
        final String saleUuid = "e00813e5-c928-4477-ba27-dacb62781d5c";
        String name = "Dummy Sale 1";
        final CreateSaleDto saleDto = new CreateSaleDto(saleUuid, name, LocalDateTime.of(2026, 01, 01, 12, 00, 00),
            LocalDateTime.of(2026, 01, 01, 13, 00, 00), SaleStatus.DRAFT, List.of(productDto));

        Mockito.doThrow(new DuplicateEntityException(saleUuid, name)).when(salesService).createFlashSale(saleDto);

        mockMvc.perform(
            post("/api/v1/admin/flash_sale")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saleDto)))
            .andExpect(status().isConflict())
            .andExpect(content().string(saleUuid));

        verify(salesService, times(1)).createFlashSale(Mockito.any(CreateSaleDto.class));
    }
}
