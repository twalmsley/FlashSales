package uk.co.aosd.flash.controllers;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.co.aosd.flash.config.TestSecurityConfig;
import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.AddFlashSaleItemDto;
import uk.co.aosd.flash.dto.FlashSaleItemDto;
import uk.co.aosd.flash.dto.FlashSaleResponseDto;
import uk.co.aosd.flash.dto.UpdateFlashSaleItemDto;
import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;
import uk.co.aosd.flash.exc.FlashSaleItemNotFoundException;
import uk.co.aosd.flash.exc.FlashSaleNotFoundException;
import uk.co.aosd.flash.exc.InsufficientResourcesException;
import uk.co.aosd.flash.exc.ProductNotFoundException;
import uk.co.aosd.flash.services.FlashSalesService;
import uk.co.aosd.flash.services.JwtTokenProvider;

/**
 * Flash Sale Admin REST API test for item management endpoints.
 */
@WebMvcTest(controllers = FlashSaleAdminRestApi.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ ErrorMapper.class, GlobalExceptionHandler.class, TestSecurityConfig.class })
@ActiveProfiles({"test", "admin-service"})
public class FlashSaleAdminRestApiItemManagementTest {

    @Autowired
    private MockMvc mockMvc;

    private static ObjectMapper objectMapper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

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

    // POST /api/v1/admin/flash_sale/{id}/items tests

    @Test
    public void shouldReturn201WhenAddingItemsToDraftSale() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);
        final var itemDto = new AddFlashSaleItemDto(UUID.randomUUID().toString(), 30, BigDecimal.valueOf(40.0));
        final var updatedSale = createTestResponseDto(saleUuid, "Test Sale", SaleStatus.DRAFT);

        Mockito.when(salesService.addItemsToFlashSale(saleUuid, List.of(itemDto))).thenReturn(updatedSale);

        mockMvc.perform(post("/api/v1/admin/flash_sale/" + saleId + "/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of(itemDto))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(saleId));

        verify(salesService, times(1)).addItemsToFlashSale(saleUuid, List.of(itemDto));
    }

    @Test
    public void shouldReturn404WhenAddingItemsToNonExistentSale() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);
        final var itemDto = new AddFlashSaleItemDto(UUID.randomUUID().toString(), 30, null);

        Mockito.doThrow(new FlashSaleNotFoundException(saleUuid))
            .when(salesService).addItemsToFlashSale(saleUuid, List.of(itemDto));

        mockMvc.perform(post("/api/v1/admin/flash_sale/" + saleId + "/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of(itemDto))))
            .andExpect(status().isNotFound());

        verify(salesService, times(1)).addItemsToFlashSale(saleUuid, List.of(itemDto));
    }

    @Test
    public void shouldReturn400WhenAddingItemsToNonDraftSale() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);
        final var itemDto = new AddFlashSaleItemDto(UUID.randomUUID().toString(), 30, null);

        Mockito.doThrow(new IllegalArgumentException("Only DRAFT flash sales can have items added"))
            .when(salesService).addItemsToFlashSale(saleUuid, List.of(itemDto));

        mockMvc.perform(post("/api/v1/admin/flash_sale/" + saleId + "/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of(itemDto))))
            .andExpect(status().isBadRequest());

        verify(salesService, times(1)).addItemsToFlashSale(saleUuid, List.of(itemDto));
    }

    @Test
    public void shouldReturn400WhenProductNotFound() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);
        final var itemDto = new AddFlashSaleItemDto(UUID.randomUUID().toString(), 30, null);

        Mockito.doThrow(new ProductNotFoundException("product-id"))
            .when(salesService).addItemsToFlashSale(saleUuid, List.of(itemDto));

        mockMvc.perform(post("/api/v1/admin/flash_sale/" + saleId + "/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of(itemDto))))
            .andExpect(status().isBadRequest());

        verify(salesService, times(1)).addItemsToFlashSale(saleUuid, List.of(itemDto));
    }

    @Test
    public void shouldReturn400WhenInsufficientStock() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);
        final var itemDto = new AddFlashSaleItemDto(UUID.randomUUID().toString(), 30, null);

        Mockito.doThrow(new InsufficientResourcesException("product-id"))
            .when(salesService).addItemsToFlashSale(saleUuid, List.of(itemDto));

        mockMvc.perform(post("/api/v1/admin/flash_sale/" + saleId + "/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of(itemDto))))
            .andExpect(status().isBadRequest());

        verify(salesService, times(1)).addItemsToFlashSale(saleUuid, List.of(itemDto));
    }

    @Test
    public void shouldReturn409WhenDuplicateProduct() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final UUID saleUuid = UUID.fromString(saleId);
        final var itemDto = new AddFlashSaleItemDto(UUID.randomUUID().toString(), 30, null);

        Mockito.doThrow(new IllegalArgumentException("Products already in sale: product-id"))
            .when(salesService).addItemsToFlashSale(saleUuid, List.of(itemDto));

        mockMvc.perform(post("/api/v1/admin/flash_sale/" + saleId + "/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of(itemDto))))
            .andExpect(status().isConflict());

        verify(salesService, times(1)).addItemsToFlashSale(saleUuid, List.of(itemDto));
    }

    @Test
    public void shouldReturn400ForInvalidUuidFormatWhenAddingItems() throws Exception {
        final String invalidSaleId = "invalid-uuid";
        final var itemDto = new AddFlashSaleItemDto(UUID.randomUUID().toString(), 30, null);

        mockMvc.perform(post("/api/v1/admin/flash_sale/" + invalidSaleId + "/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of(itemDto))))
            .andExpect(status().isBadRequest());

        verify(salesService, times(0)).addItemsToFlashSale(Mockito.any(UUID.class), Mockito.any());
    }

    // PUT /api/v1/admin/flash_sale/{id}/items/{itemId} tests

    @Test
    public void shouldReturn200WhenUpdatingItemAllocatedStock() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final String itemId = "f11924f6-d039-5588-cb38-ebdc73892e6d";
        final UUID saleUuid = UUID.fromString(saleId);
        final UUID itemUuid = UUID.fromString(itemId);
        final var updateDto = new UpdateFlashSaleItemDto(50, null);
        final var updatedSale = createTestResponseDto(saleUuid, "Test Sale", SaleStatus.DRAFT);

        Mockito.when(salesService.updateFlashSaleItem(saleUuid, itemUuid, updateDto)).thenReturn(updatedSale);

        mockMvc.perform(put("/api/v1/admin/flash_sale/" + saleId + "/items/" + itemId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(saleId));

        verify(salesService, times(1)).updateFlashSaleItem(saleUuid, itemUuid, updateDto);
    }

    @Test
    public void shouldReturn200WhenUpdatingItemSalePrice() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final String itemId = "f11924f6-d039-5588-cb38-ebdc73892e6d";
        final UUID saleUuid = UUID.fromString(saleId);
        final UUID itemUuid = UUID.fromString(itemId);
        final var updateDto = new UpdateFlashSaleItemDto(null, BigDecimal.valueOf(35.0));
        final var updatedSale = createTestResponseDto(saleUuid, "Test Sale", SaleStatus.DRAFT);

        Mockito.when(salesService.updateFlashSaleItem(saleUuid, itemUuid, updateDto)).thenReturn(updatedSale);

        mockMvc.perform(put("/api/v1/admin/flash_sale/" + saleId + "/items/" + itemId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk());

        verify(salesService, times(1)).updateFlashSaleItem(saleUuid, itemUuid, updateDto);
    }

    @Test
    public void shouldReturn200WhenUpdatingBothFields() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final String itemId = "f11924f6-d039-5588-cb38-ebdc73892e6d";
        final UUID saleUuid = UUID.fromString(saleId);
        final UUID itemUuid = UUID.fromString(itemId);
        final var updateDto = new UpdateFlashSaleItemDto(50, BigDecimal.valueOf(35.0));
        final var updatedSale = createTestResponseDto(saleUuid, "Test Sale", SaleStatus.DRAFT);

        Mockito.when(salesService.updateFlashSaleItem(saleUuid, itemUuid, updateDto)).thenReturn(updatedSale);

        mockMvc.perform(put("/api/v1/admin/flash_sale/" + saleId + "/items/" + itemId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk());

        verify(salesService, times(1)).updateFlashSaleItem(saleUuid, itemUuid, updateDto);
    }

    @Test
    public void shouldReturn404WhenUpdatingItemInNonExistentSale() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final String itemId = "f11924f6-d039-5588-cb38-ebdc73892e6d";
        final UUID saleUuid = UUID.fromString(saleId);
        final UUID itemUuid = UUID.fromString(itemId);
        final var updateDto = new UpdateFlashSaleItemDto(50, null);

        Mockito.doThrow(new FlashSaleNotFoundException(saleUuid))
            .when(salesService).updateFlashSaleItem(saleUuid, itemUuid, updateDto);

        mockMvc.perform(put("/api/v1/admin/flash_sale/" + saleId + "/items/" + itemId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isNotFound());

        verify(salesService, times(1)).updateFlashSaleItem(saleUuid, itemUuid, updateDto);
    }

    @Test
    public void shouldReturn404WhenUpdatingNonExistentItem() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final String itemId = "f11924f6-d039-5588-cb38-ebdc73892e6d";
        final UUID saleUuid = UUID.fromString(saleId);
        final UUID itemUuid = UUID.fromString(itemId);
        final var updateDto = new UpdateFlashSaleItemDto(50, null);

        Mockito.doThrow(new FlashSaleItemNotFoundException(itemUuid))
            .when(salesService).updateFlashSaleItem(saleUuid, itemUuid, updateDto);

        mockMvc.perform(put("/api/v1/admin/flash_sale/" + saleId + "/items/" + itemId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isNotFound());

        verify(salesService, times(1)).updateFlashSaleItem(saleUuid, itemUuid, updateDto);
    }

    @Test
    public void shouldReturn400WhenUpdatingItemInNonDraftSale() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final String itemId = "f11924f6-d039-5588-cb38-ebdc73892e6d";
        final UUID saleUuid = UUID.fromString(saleId);
        final UUID itemUuid = UUID.fromString(itemId);
        final var updateDto = new UpdateFlashSaleItemDto(50, null);

        Mockito.doThrow(new IllegalArgumentException("Only DRAFT flash sales can have items updated"))
            .when(salesService).updateFlashSaleItem(saleUuid, itemUuid, updateDto);

        mockMvc.perform(put("/api/v1/admin/flash_sale/" + saleId + "/items/" + itemId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isBadRequest());

        verify(salesService, times(1)).updateFlashSaleItem(saleUuid, itemUuid, updateDto);
    }

    @Test
    public void shouldReturn400WhenInsufficientStockForUpdate() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final String itemId = "f11924f6-d039-5588-cb38-ebdc73892e6d";
        final UUID saleUuid = UUID.fromString(saleId);
        final UUID itemUuid = UUID.fromString(itemId);
        final var updateDto = new UpdateFlashSaleItemDto(50, null);

        Mockito.doThrow(new InsufficientResourcesException("product-id"))
            .when(salesService).updateFlashSaleItem(saleUuid, itemUuid, updateDto);

        mockMvc.perform(put("/api/v1/admin/flash_sale/" + saleId + "/items/" + itemId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isBadRequest());

        verify(salesService, times(1)).updateFlashSaleItem(saleUuid, itemUuid, updateDto);
    }

    @Test
    public void shouldReturn400ForInvalidUuidFormatWhenUpdating() throws Exception {
        final String invalidSaleId = "invalid-uuid";
        final String itemId = "f11924f6-d039-5588-cb38-ebdc73892e6d";
        final var updateDto = new UpdateFlashSaleItemDto(50, null);

        mockMvc.perform(put("/api/v1/admin/flash_sale/" + invalidSaleId + "/items/" + itemId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isBadRequest());

        verify(salesService, times(0)).updateFlashSaleItem(Mockito.any(UUID.class), Mockito.any(UUID.class), Mockito.any());
    }

    // DELETE /api/v1/admin/flash_sale/{id}/items/{itemId} tests

    @Test
    public void shouldReturn200WhenRemovingItemFromDraftSale() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final String itemId = "f11924f6-d039-5588-cb38-ebdc73892e6d";
        final UUID saleUuid = UUID.fromString(saleId);
        final UUID itemUuid = UUID.fromString(itemId);
        final var updatedSale = createTestResponseDto(saleUuid, "Test Sale", SaleStatus.DRAFT);

        Mockito.when(salesService.removeFlashSaleItem(saleUuid, itemUuid)).thenReturn(updatedSale);

        mockMvc.perform(delete("/api/v1/admin/flash_sale/" + saleId + "/items/" + itemId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(saleId));

        verify(salesService, times(1)).removeFlashSaleItem(saleUuid, itemUuid);
    }

    @Test
    public void shouldReturn404WhenRemovingItemFromNonExistentSale() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final String itemId = "f11924f6-d039-5588-cb38-ebdc73892e6d";
        final UUID saleUuid = UUID.fromString(saleId);
        final UUID itemUuid = UUID.fromString(itemId);

        Mockito.doThrow(new FlashSaleNotFoundException(saleUuid))
            .when(salesService).removeFlashSaleItem(saleUuid, itemUuid);

        mockMvc.perform(delete("/api/v1/admin/flash_sale/" + saleId + "/items/" + itemId))
            .andExpect(status().isNotFound());

        verify(salesService, times(1)).removeFlashSaleItem(saleUuid, itemUuid);
    }

    @Test
    public void shouldReturn404WhenRemovingNonExistentItem() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final String itemId = "f11924f6-d039-5588-cb38-ebdc73892e6d";
        final UUID saleUuid = UUID.fromString(saleId);
        final UUID itemUuid = UUID.fromString(itemId);

        Mockito.doThrow(new FlashSaleItemNotFoundException(itemUuid))
            .when(salesService).removeFlashSaleItem(saleUuid, itemUuid);

        mockMvc.perform(delete("/api/v1/admin/flash_sale/" + saleId + "/items/" + itemId))
            .andExpect(status().isNotFound());

        verify(salesService, times(1)).removeFlashSaleItem(saleUuid, itemUuid);
    }

    @Test
    public void shouldReturn400WhenRemovingItemFromNonDraftSale() throws Exception {
        final String saleId = "e00813e5-c928-4477-ba27-dacb62781d5c";
        final String itemId = "f11924f6-d039-5588-cb38-ebdc73892e6d";
        final UUID saleUuid = UUID.fromString(saleId);
        final UUID itemUuid = UUID.fromString(itemId);

        Mockito.doThrow(new IllegalArgumentException("Only DRAFT flash sales can have items removed"))
            .when(salesService).removeFlashSaleItem(saleUuid, itemUuid);

        mockMvc.perform(delete("/api/v1/admin/flash_sale/" + saleId + "/items/" + itemId))
            .andExpect(status().isBadRequest());

        verify(salesService, times(1)).removeFlashSaleItem(saleUuid, itemUuid);
    }

    @Test
    public void shouldReturn400ForInvalidUuidFormatWhenRemoving() throws Exception {
        final String invalidSaleId = "invalid-uuid";
        final String itemId = "f11924f6-d039-5588-cb38-ebdc73892e6d";

        mockMvc.perform(delete("/api/v1/admin/flash_sale/" + invalidSaleId + "/items/" + itemId))
            .andExpect(status().isBadRequest());

        verify(salesService, times(0)).removeFlashSaleItem(Mockito.any(UUID.class), Mockito.any(UUID.class));
    }

    // Helper methods

    private FlashSaleResponseDto createTestResponseDto(final UUID id, final String title, final SaleStatus status) {
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        final List<FlashSaleItemDto> items = List.of();
        return new FlashSaleResponseDto(id.toString(), title, startTime, endTime, status, items);
    }
}
