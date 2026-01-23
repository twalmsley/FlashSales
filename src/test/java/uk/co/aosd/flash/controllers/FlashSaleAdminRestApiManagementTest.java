package uk.co.aosd.flash.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

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
import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.FlashSaleItemDto;
import uk.co.aosd.flash.dto.FlashSaleResponseDto;
import uk.co.aosd.flash.dto.UpdateFlashSaleDto;
import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;
import uk.co.aosd.flash.exc.FlashSaleNotFoundException;
import uk.co.aosd.flash.exc.InvalidSaleTimesException;
import uk.co.aosd.flash.exc.SaleDurationTooShortException;
import uk.co.aosd.flash.services.FlashSalesService;

/**
 * Flash Sale Admin REST API test for management endpoints.
 */
@WebMvcTest(FlashSaleAdminRestApi.class)
@Import({ ErrorMapper.class, GlobalExceptionHandler.class })
public class FlashSaleAdminRestApiManagementTest {

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

    // GET /api/v1/admin/flash_sale tests

    @Test
    public void shouldReturn200WithListOfSalesNoFilters() throws Exception {
        final var sale1 = createTestResponseDto(UUID.randomUUID(), "Sale 1", SaleStatus.DRAFT);
        final var sale2 = createTestResponseDto(UUID.randomUUID(), "Sale 2", SaleStatus.ACTIVE);
        final List<FlashSaleResponseDto> sales = List.of(sale1, sale2);

        Mockito.when(salesService.getAllFlashSales(null, null, null)).thenReturn(sales);

        mockMvc.perform(get("/api/v1/admin/flash_sale"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));

        verify(salesService, times(1)).getAllFlashSales(null, null, null);
    }

    @Test
    public void shouldReturn200WithFilteredResultsStatusOnly() throws Exception {
        final var sale1 = createTestResponseDto(UUID.randomUUID(), "Draft Sale", SaleStatus.DRAFT);
        final List<FlashSaleResponseDto> sales = List.of(sale1);

        Mockito.when(salesService.getAllFlashSales(SaleStatus.DRAFT, null, null)).thenReturn(sales);

        mockMvc.perform(get("/api/v1/admin/flash_sale")
                .param("status", "DRAFT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("DRAFT"));

        verify(salesService, times(1)).getAllFlashSales(SaleStatus.DRAFT, null, null);
    }

    @Test
    public void shouldReturn200WithFilteredResultsDateRangeOnly() throws Exception {
        final OffsetDateTime startDate = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endDate = OffsetDateTime.of(2026, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        final var sale1 = createTestResponseDto(UUID.randomUUID(), "Sale 1", SaleStatus.DRAFT);
        final List<FlashSaleResponseDto> sales = List.of(sale1);

        Mockito.when(salesService.getAllFlashSales(null, startDate, endDate)).thenReturn(sales);

        mockMvc.perform(get("/api/v1/admin/flash_sale")
                .param("startDate", "2026-01-01T00:00:00Z")
                .param("endDate", "2026-01-31T23:59:59Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));

        verify(salesService, times(1)).getAllFlashSales(null, startDate, endDate);
    }

    @Test
    public void shouldReturn200WithFilteredResultsStatusAndDateRange() throws Exception {
        final OffsetDateTime startDate = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endDate = OffsetDateTime.of(2026, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        final var sale1 = createTestResponseDto(UUID.randomUUID(), "Draft Sale", SaleStatus.DRAFT);
        final List<FlashSaleResponseDto> sales = List.of(sale1);

        Mockito.when(salesService.getAllFlashSales(SaleStatus.DRAFT, startDate, endDate)).thenReturn(sales);

        mockMvc.perform(get("/api/v1/admin/flash_sale")
                .param("status", "DRAFT")
                .param("startDate", "2026-01-01T00:00:00Z")
                .param("endDate", "2026-01-31T23:59:59Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("DRAFT"));

        verify(salesService, times(1)).getAllFlashSales(SaleStatus.DRAFT, startDate, endDate);
    }

    @Test
    public void shouldReturn400ForInvalidStatusEnumValue() throws Exception {
        mockMvc.perform(get("/api/v1/admin/flash_sale")
                .param("status", "INVALID_STATUS"))
            .andExpect(status().isBadRequest());

        verify(salesService, times(0)).getAllFlashSales(Mockito.any(), Mockito.any(), Mockito.any());
    }

    // GET /api/v1/admin/flash_sale/{id} tests

    @Test
    public void shouldReturn200WithFlashSaleDtoWhenFound() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);
        final var sale = createTestResponseDto(saleUuid, "Test Sale", SaleStatus.DRAFT);

        Mockito.when(salesService.getFlashSaleById(saleUuid)).thenReturn(sale);

        mockMvc.perform(get("/api/v1/admin/flash_sale/" + saleId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(saleId))
            .andExpect(jsonPath("$.title").value("Test Sale"))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(salesService, times(1)).getFlashSaleById(saleUuid);
    }

    @Test
    public void shouldReturn404WhenNotFound() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);

        Mockito.doThrow(new FlashSaleNotFoundException(saleUuid))
            .when(salesService).getFlashSaleById(saleUuid);

        mockMvc.perform(get("/api/v1/admin/flash_sale/" + saleId))
            .andExpect(status().isNotFound());

        verify(salesService, times(1)).getFlashSaleById(saleUuid);
    }

    @Test
    public void shouldReturn400ForInvalidUuidFormat() throws Exception {
        final String invalidSaleId = "invalid-uuid";

        mockMvc.perform(get("/api/v1/admin/flash_sale/" + invalidSaleId))
            .andExpect(status().isBadRequest());

        verify(salesService, times(0)).getFlashSaleById(Mockito.any(UUID.class));
    }

    // PUT /api/v1/admin/flash_sale/{id} tests

    @Test
    public void shouldReturn200WithUpdatedDtoTitleOnly() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);
        final UpdateFlashSaleDto updateDto = new UpdateFlashSaleDto("New Title", null, null);
        final var updatedSale = createTestResponseDto(saleUuid, "New Title", SaleStatus.DRAFT);

        Mockito.when(salesService.updateFlashSale(saleUuid, updateDto)).thenReturn(updatedSale);

        mockMvc.perform(put("/api/v1/admin/flash_sale/" + saleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("New Title"));

        verify(salesService, times(1)).updateFlashSale(saleUuid, updateDto);
    }

    @Test
    public void shouldReturn200WithUpdatedDtoTimesOnly() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);
        final OffsetDateTime newStartTime = OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime newEndTime = OffsetDateTime.of(2026, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        final UpdateFlashSaleDto updateDto = new UpdateFlashSaleDto(null, newStartTime, newEndTime);
        final var updatedSale = createTestResponseDto(saleUuid, "Test Sale", SaleStatus.DRAFT);

        Mockito.when(salesService.updateFlashSale(saleUuid, updateDto)).thenReturn(updatedSale);

        mockMvc.perform(put("/api/v1/admin/flash_sale/" + saleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk());

        verify(salesService, times(1)).updateFlashSale(saleUuid, updateDto);
    }

    @Test
    public void shouldReturn200WithUpdatedDtoAllFields() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);
        final OffsetDateTime newStartTime = OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime newEndTime = OffsetDateTime.of(2026, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        final UpdateFlashSaleDto updateDto = new UpdateFlashSaleDto("New Title", newStartTime, newEndTime);
        final var updatedSale = createTestResponseDto(saleUuid, "New Title", SaleStatus.DRAFT);

        Mockito.when(salesService.updateFlashSale(saleUuid, updateDto)).thenReturn(updatedSale);

        mockMvc.perform(put("/api/v1/admin/flash_sale/" + saleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("New Title"));

        verify(salesService, times(1)).updateFlashSale(saleUuid, updateDto);
    }

    @Test
    public void shouldReturn404WhenUpdatingNonExistentSale() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);
        final UpdateFlashSaleDto updateDto = new UpdateFlashSaleDto("New Title", null, null);

        Mockito.doThrow(new FlashSaleNotFoundException(saleUuid))
            .when(salesService).updateFlashSale(saleUuid, updateDto);

        mockMvc.perform(put("/api/v1/admin/flash_sale/" + saleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isNotFound());

        verify(salesService, times(1)).updateFlashSale(saleUuid, updateDto);
    }

    @Test
    public void shouldReturn400ForInvalidUuidFormatWhenUpdating() throws Exception {
        final String invalidSaleId = "invalid-uuid";
        final UpdateFlashSaleDto updateDto = new UpdateFlashSaleDto("New Title", null, null);

        mockMvc.perform(put("/api/v1/admin/flash_sale/" + invalidSaleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isBadRequest());

        verify(salesService, times(0)).updateFlashSale(Mockito.any(UUID.class), Mockito.any(UpdateFlashSaleDto.class));
    }

    @Test
    public void shouldReturn400ForValidationErrorsEndTimeBeforeStartTime() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = OffsetDateTime.of(2026, 1, 15, 11, 0, 0, 0, ZoneOffset.UTC);
        final UpdateFlashSaleDto updateDto = new UpdateFlashSaleDto(null, startTime, endTime);

        Mockito.doThrow(new InvalidSaleTimesException(startTime, endTime))
            .when(salesService).updateFlashSale(saleUuid, updateDto);

        mockMvc.perform(put("/api/v1/admin/flash_sale/" + saleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isBadRequest());

        verify(salesService, times(1)).updateFlashSale(saleUuid, updateDto);
    }

    @Test
    public void shouldReturn400ForValidationErrorsDurationTooShort() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = OffsetDateTime.of(2026, 1, 15, 12, 5, 0, 0, ZoneOffset.UTC);
        final UpdateFlashSaleDto updateDto = new UpdateFlashSaleDto(null, startTime, endTime);

        Mockito.doThrow(new SaleDurationTooShortException("Too short", 5.0f, 10.0f))
            .when(salesService).updateFlashSale(saleUuid, updateDto);

        mockMvc.perform(put("/api/v1/admin/flash_sale/" + saleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isBadRequest());

        verify(salesService, times(1)).updateFlashSale(saleUuid, updateDto);
    }

    // DELETE /api/v1/admin/flash_sale/{id} tests

    @Test
    public void shouldReturn204NoContentForDraftSale() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);

        doNothing().when(salesService).deleteFlashSale(saleUuid);

        mockMvc.perform(delete("/api/v1/admin/flash_sale/" + saleId))
            .andExpect(status().isNoContent());

        verify(salesService, times(1)).deleteFlashSale(saleUuid);
    }

    @Test
    public void shouldReturn404WhenDeletingNonExistentSale() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);

        doThrow(new FlashSaleNotFoundException(saleUuid))
            .when(salesService).deleteFlashSale(saleUuid);

        mockMvc.perform(delete("/api/v1/admin/flash_sale/" + saleId))
            .andExpect(status().isNotFound());

        verify(salesService, times(1)).deleteFlashSale(saleUuid);
    }

    @Test
    public void shouldReturn400ForInvalidUuidFormatWhenDeleting() throws Exception {
        final String invalidSaleId = "invalid-uuid";

        mockMvc.perform(delete("/api/v1/admin/flash_sale/" + invalidSaleId))
            .andExpect(status().isBadRequest());

        verify(salesService, times(0)).deleteFlashSale(Mockito.any(UUID.class));
    }

    @Test
    public void shouldReturn400ForActiveStatus() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);

        doThrow(new IllegalArgumentException("Only DRAFT flash sales can be deleted"))
            .when(salesService).deleteFlashSale(saleUuid);

        mockMvc.perform(delete("/api/v1/admin/flash_sale/" + saleId))
            .andExpect(status().isBadRequest());

        verify(salesService, times(1)).deleteFlashSale(saleUuid);
    }

    @Test
    public void shouldReturn400ForCompletedStatus() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);

        doThrow(new IllegalArgumentException("Only DRAFT flash sales can be deleted"))
            .when(salesService).deleteFlashSale(saleUuid);

        mockMvc.perform(delete("/api/v1/admin/flash_sale/" + saleId))
            .andExpect(status().isBadRequest());

        verify(salesService, times(1)).deleteFlashSale(saleUuid);
    }

    @Test
    public void shouldReturn400ForCancelledStatus() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);

        doThrow(new IllegalArgumentException("Only DRAFT flash sales can be deleted"))
            .when(salesService).deleteFlashSale(saleUuid);

        mockMvc.perform(delete("/api/v1/admin/flash_sale/" + saleId))
            .andExpect(status().isBadRequest());

        verify(salesService, times(1)).deleteFlashSale(saleUuid);
    }

    // Helper methods

    private FlashSaleResponseDto createTestResponseDto(final UUID id, final String title, final SaleStatus status) {
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        final List<FlashSaleItemDto> items = List.of();
        return new FlashSaleResponseDto(id.toString(), title, startTime, endTime, status, items);
    }
}
