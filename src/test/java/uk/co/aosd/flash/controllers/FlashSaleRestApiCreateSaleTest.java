package uk.co.aosd.flash.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.CreateSaleDto;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;

@WebMvcTest(FlashSaleRestApi.class)
@Import({ ErrorMapper.class, GlobalExceptionHandler.class })
public class FlashSaleRestApiCreateSaleTest {

    @Autowired
    private MockMvc mockMvc;

    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void beforeAll() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    public void beforeEach() {
    }

    @Test
    public void shouldCreateAFlashSaleSuccessfully() throws Exception {
        final String productUuid = "846a8892-422b-4eff-a201-509bce782cb9";
        final ProductDto productDto = new ProductDto(productUuid, "Dummy Product 1", "Dummy product 1 description", 101,
            BigDecimal.valueOf(99.99));
        final CreateSaleDto saleDto = new CreateSaleDto(null, "Dummy Sale 1", LocalDateTime.of(2026, 01, 01, 12, 00, 00),
            LocalDateTime.of(2026, 01, 01, 13, 00, 00), SaleStatus.DRAFT, new ProductDto[] { productDto });

        mockMvc.perform(
            post("/api/v1/admin/flash_sale")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saleDto)))
            .andExpect(status().isOk())
            .andExpect(content().string("Dummy Sale 1"));
    }

}
