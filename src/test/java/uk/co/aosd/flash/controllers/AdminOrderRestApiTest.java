package uk.co.aosd.flash.controllers;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.co.aosd.flash.config.TestSecurityConfig;
import uk.co.aosd.flash.domain.OrderStatus;
import uk.co.aosd.flash.dto.OrderDetailDto;
import uk.co.aosd.flash.dto.UpdateOrderStatusDto;
import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;
import uk.co.aosd.flash.exc.InvalidOrderStatusException;
import uk.co.aosd.flash.exc.OrderNotFoundException;
import uk.co.aosd.flash.services.JwtTokenProvider;
import uk.co.aosd.flash.services.OrderService;

/**
 * Admin Order REST API test.
 */
@WebMvcTest(controllers = AdminOrderRestApi.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ ErrorMapper.class, GlobalExceptionHandler.class, TestSecurityConfig.class })
@ActiveProfiles({"test", "admin-service"})
public class AdminOrderRestApiTest {

    @Autowired
    private MockMvc mockMvc;

    private static ObjectMapper objectMapper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private OrderService orderService;

    @BeforeAll
    public static void beforeAll() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(orderService);
    }

    // GET /api/v1/admin/orders tests

    @Test
    public void shouldReturn200WithListOfOrdersNoFilters() throws Exception {
        final UUID orderId1 = UUID.randomUUID();
        final UUID orderId2 = UUID.randomUUID();
        final OrderDetailDto order1 = createTestOrderDto(orderId1, OrderStatus.PENDING);
        final OrderDetailDto order2 = createTestOrderDto(orderId2, OrderStatus.PAID);
        final List<OrderDetailDto> orders = List.of(order1, order2);

        when(orderService.getAllOrders(null, null, null, null)).thenReturn(orders);

        mockMvc.perform(get("/api/v1/admin/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));

        verify(orderService, times(1)).getAllOrders(null, null, null, null);
    }

    @Test
    public void shouldReturn200WithFilteredResultsStatusOnly() throws Exception {
        final UUID orderId = UUID.randomUUID();
        final OrderDetailDto order = createTestOrderDto(orderId, OrderStatus.PAID);
        final List<OrderDetailDto> orders = List.of(order);

        when(orderService.getAllOrders(OrderStatus.PAID, null, null, null)).thenReturn(orders);

        mockMvc.perform(get("/api/v1/admin/orders")
            .param("status", "PAID"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("PAID"));

        verify(orderService, times(1)).getAllOrders(OrderStatus.PAID, null, null, null);
    }

    @Test
    public void shouldReturn200WithFilteredResultsDateRangeOnly() throws Exception {
        final OffsetDateTime startDate = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endDate = OffsetDateTime.of(2026, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        final UUID orderId = UUID.randomUUID();
        final OrderDetailDto order = createTestOrderDto(orderId, OrderStatus.PENDING);
        final List<OrderDetailDto> orders = List.of(order);

        when(orderService.getAllOrders(null, startDate, endDate, null)).thenReturn(orders);

        mockMvc.perform(get("/api/v1/admin/orders")
            .param("startDate", "2026-01-01T00:00:00Z")
            .param("endDate", "2026-01-31T23:59:59Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));

        verify(orderService, times(1)).getAllOrders(null, startDate, endDate, null);
    }

    @Test
    public void shouldReturn200WithFilteredResultsUserIdOnly() throws Exception {
        final UUID userId = UUID.randomUUID();
        final UUID orderId = UUID.randomUUID();
        final OrderDetailDto order = createTestOrderDto(orderId, OrderStatus.PAID);
        final List<OrderDetailDto> orders = List.of(order);

        when(orderService.getAllOrders(null, null, null, userId)).thenReturn(orders);

        mockMvc.perform(get("/api/v1/admin/orders")
            .param("userId", userId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));

        verify(orderService, times(1)).getAllOrders(null, null, null, userId);
    }

    @Test
    public void shouldReturn200WithAllFilters() throws Exception {
        final OffsetDateTime startDate = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endDate = OffsetDateTime.of(2026, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        final UUID userId = UUID.randomUUID();
        final UUID orderId = UUID.randomUUID();
        final OrderDetailDto order = createTestOrderDto(orderId, OrderStatus.PAID);
        final List<OrderDetailDto> orders = List.of(order);

        when(orderService.getAllOrders(OrderStatus.PAID, startDate, endDate, userId)).thenReturn(orders);

        mockMvc.perform(get("/api/v1/admin/orders")
            .param("status", "PAID")
            .param("startDate", "2026-01-01T00:00:00Z")
            .param("endDate", "2026-01-31T23:59:59Z")
            .param("userId", userId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("PAID"));

        verify(orderService, times(1)).getAllOrders(OrderStatus.PAID, startDate, endDate, userId);
    }

    @Test
    public void shouldReturn400ForInvalidStatusEnumValue() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders")
            .param("status", "INVALID_STATUS"))
            .andExpect(status().isBadRequest());

        verify(orderService, times(0)).getAllOrders(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void shouldReturn400ForInvalidUserIdFormat() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders")
            .param("userId", "invalid-uuid"))
            .andExpect(status().isBadRequest());

        verify(orderService, times(0)).getAllOrders(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void shouldReturn400ForInvalidDateRange() throws Exception {
        when(orderService.getAllOrders(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenThrow(new IllegalArgumentException("Start date must be before or equal to end date"));

        mockMvc.perform(get("/api/v1/admin/orders")
            .param("startDate", "2026-01-31T23:59:59Z")
            .param("endDate", "2026-01-01T00:00:00Z"))
            .andExpect(status().isBadRequest());

        verify(orderService, times(1)).getAllOrders(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    // GET /api/v1/admin/orders/{id} tests

    @Test
    public void shouldReturn200WithOrderDtoWhenFound() throws Exception {
        final String orderId = "2b8efb9f-6f89-4b2d-8c73-4b2f9d4d2e1a";
        final UUID orderUuid = UUID.fromString(orderId);
        final OrderDetailDto order = createTestOrderDto(orderUuid, OrderStatus.PAID);

        when(orderService.getOrderByIdForAdmin(orderUuid)).thenReturn(order);

        mockMvc.perform(get("/api/v1/admin/orders/" + orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(orderId))
            .andExpect(jsonPath("$.status").value("PAID"));

        verify(orderService, times(1)).getOrderByIdForAdmin(orderUuid);
    }

    @Test
    public void shouldReturn404WhenNotFound() throws Exception {
        final String orderId = "2b8efb9f-6f89-4b2d-8c73-4b2f9d4d2e1a";
        final UUID orderUuid = UUID.fromString(orderId);

        doThrow(new OrderNotFoundException(orderUuid))
            .when(orderService).getOrderByIdForAdmin(orderUuid);

        mockMvc.perform(get("/api/v1/admin/orders/" + orderId))
            .andExpect(status().isNotFound());

        verify(orderService, times(1)).getOrderByIdForAdmin(orderUuid);
    }

    @Test
    public void shouldReturn400ForInvalidUuidFormat() throws Exception {
        final String invalidOrderId = "invalid-uuid";

        mockMvc.perform(get("/api/v1/admin/orders/" + invalidOrderId))
            .andExpect(status().isBadRequest());

        verify(orderService, times(0)).getOrderByIdForAdmin(Mockito.any(UUID.class));
    }

    // PUT /api/v1/admin/orders/{id}/status tests

    @Test
    public void shouldReturn200WhenUpdatingStatusSuccessfully() throws Exception {
        final String orderId = "2b8efb9f-6f89-4b2d-8c73-4b2f9d4d2e1a";
        final UUID orderUuid = UUID.fromString(orderId);
        final UpdateOrderStatusDto updateDto = new UpdateOrderStatusDto(OrderStatus.DISPATCHED);

        doNothing().when(orderService).updateOrderStatus(orderUuid, OrderStatus.DISPATCHED);

        mockMvc.perform(put("/api/v1/admin/orders/" + orderId + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk());

        verify(orderService, times(1)).updateOrderStatus(orderUuid, OrderStatus.DISPATCHED);
    }

    @Test
    public void shouldReturn404WhenOrderNotFound() throws Exception {
        final String orderId = "2b8efb9f-6f89-4b2d-8c73-4b2f9d4d2e1a";
        final UUID orderUuid = UUID.fromString(orderId);
        final UpdateOrderStatusDto updateDto = new UpdateOrderStatusDto(OrderStatus.DISPATCHED);

        doThrow(new OrderNotFoundException(orderUuid))
            .when(orderService).updateOrderStatus(orderUuid, OrderStatus.DISPATCHED);

        mockMvc.perform(put("/api/v1/admin/orders/" + orderId + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isNotFound());

        verify(orderService, times(1)).updateOrderStatus(orderUuid, OrderStatus.DISPATCHED);
    }

    @Test
    public void shouldReturn400ForInvalidUuidFormatWhenUpdatingStatus() throws Exception {
        final String invalidOrderId = "invalid-uuid";
        final UpdateOrderStatusDto updateDto = new UpdateOrderStatusDto(OrderStatus.DISPATCHED);

        mockMvc.perform(put("/api/v1/admin/orders/" + invalidOrderId + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isBadRequest());

        verify(orderService, times(0)).updateOrderStatus(Mockito.any(UUID.class), Mockito.any(OrderStatus.class));
    }

    @Test
    public void shouldReturn400ForInvalidStatusTransition() throws Exception {
        final String orderId = "2b8efb9f-6f89-4b2d-8c73-4b2f9d4d2e1a";
        final UUID orderUuid = UUID.fromString(orderId);
        final UpdateOrderStatusDto updateDto = new UpdateOrderStatusDto(OrderStatus.DISPATCHED);

        doThrow(new InvalidOrderStatusException(orderUuid, OrderStatus.PENDING, OrderStatus.DISPATCHED, "status update"))
            .when(orderService).updateOrderStatus(orderUuid, OrderStatus.DISPATCHED);

        mockMvc.perform(put("/api/v1/admin/orders/" + orderId + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isBadRequest());

        verify(orderService, times(1)).updateOrderStatus(orderUuid, OrderStatus.DISPATCHED);
    }

    @Test
    public void shouldReturn500WhenStockOperationFails() throws Exception {
        final String orderId = "2b8efb9f-6f89-4b2d-8c73-4b2f9d4d2e1a";
        final UUID orderUuid = UUID.fromString(orderId);
        final UpdateOrderStatusDto updateDto = new UpdateOrderStatusDto(OrderStatus.DISPATCHED);

        doThrow(new IllegalStateException("Failed to decrement product stock for dispatch"))
            .when(orderService).updateOrderStatus(orderUuid, OrderStatus.DISPATCHED);

        mockMvc.perform(put("/api/v1/admin/orders/" + orderId + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isInternalServerError());

        verify(orderService, times(1)).updateOrderStatus(orderUuid, OrderStatus.DISPATCHED);
    }

    @Test
    public void shouldReturn422ForValidationError() throws Exception {
        final String orderId = "2b8efb9f-6f89-4b2d-8c73-4b2f9d4d2e1a";
        final UpdateOrderStatusDto updateDto = new UpdateOrderStatusDto(null); // null status should fail validation

        mockMvc.perform(put("/api/v1/admin/orders/" + orderId + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isUnprocessableEntity()); // Spring validation returns 422

        verify(orderService, times(0)).updateOrderStatus(Mockito.any(UUID.class), Mockito.any(OrderStatus.class));
    }

    // Helper methods

    private OrderDetailDto createTestOrderDto(final UUID orderId, final OrderStatus status) {
        final UUID userId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final UUID flashSaleItemId = UUID.randomUUID();
        final UUID flashSaleId = UUID.randomUUID();
        final BigDecimal soldPrice = BigDecimal.valueOf(99.99);
        final Integer soldQuantity = 2;
        final BigDecimal totalAmount = soldPrice.multiply(BigDecimal.valueOf(soldQuantity));
        final OffsetDateTime createdAt = OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC);

        return new OrderDetailDto(
            orderId,
            userId,
            productId,
            "Test Product",
            flashSaleItemId,
            flashSaleId,
            "Test Sale",
            soldPrice,
            soldQuantity,
            totalAmount,
            status,
            createdAt);
    }
}
