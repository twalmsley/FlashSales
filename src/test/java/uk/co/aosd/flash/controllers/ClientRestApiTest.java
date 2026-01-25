package uk.co.aosd.flash.controllers;

// Static imports for the fluent API (crucial for readability)
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
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
import uk.co.aosd.flash.dto.ClientActiveSaleDto;
import uk.co.aosd.flash.dto.ClientDraftSaleDto;
import uk.co.aosd.flash.dto.ClientProductDto;
import uk.co.aosd.flash.dto.CreateOrderDto;
import uk.co.aosd.flash.dto.OrderDetailDto;
import uk.co.aosd.flash.dto.OrderResponseDto;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;
import uk.co.aosd.flash.exc.OrderNotFoundException;
import uk.co.aosd.flash.services.ActiveSalesService;
import uk.co.aosd.flash.services.DraftSalesService;
import uk.co.aosd.flash.services.JwtTokenProvider;
import uk.co.aosd.flash.services.OrderService;
import uk.co.aosd.flash.services.ProductsService;

/**
 * Admin API Web Test.
 */
@WebMvcTest(controllers = ClientRestApi.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ ErrorMapper.class, GlobalExceptionHandler.class, TestSecurityConfig.class })
@ActiveProfiles({"test", "api-service"})
public class ClientRestApiTest {

    @Autowired
    private MockMvc mockMvc;

    private static ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private ProductsService productsService;

    @MockitoBean
    private ActiveSalesService activeSalesService;

    @MockitoBean
    private DraftSalesService draftSalesService;

    @MockitoBean
    private OrderService orderService;

    @BeforeAll
    public static void beforeAll() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(productsService, activeSalesService, draftSalesService, orderService);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor withUser(final UUID userId) {
        final var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            userId, null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
        authentication.setAuthenticated(true);
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

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

    @Test
    public void shouldReturnActiveSalesSuccessfully() throws Exception {
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = startTime.plusHours(1);

        final ClientActiveSaleDto sale1 = new ClientActiveSaleDto(
            "547cf74d-7b64-44ea-b70f-cbcde09cadc9",
            "ab3b715e-e2c2-4c28-925d-83ac93c32d02",
            "Sale 1",
            startTime,
            endTime,
            "11111111-1111-1111-1111-111111111111",
            10,
            5,
            BigDecimal.valueOf(89.99));

        final ClientActiveSaleDto sale2 = new ClientActiveSaleDto(
            "1c05690e-cd9a-42ee-9f15-194b4c454216",
            "d4e5f6a7-b8c9-d0e1-f2a3-b4c5d6e7f8a9",
            "Sale 2",
            startTime.plusDays(1),
            endTime.plusDays(1),
            "22222222-2222-2222-2222-222222222222",
            20,
            10,
            BigDecimal.valueOf(79.99));

        final List<ClientActiveSaleDto> activeSales = List.of(sale1, sale2);

        Mockito.when(activeSalesService.getActiveSales()).thenReturn(activeSales);

        final var getResult = mockMvc.perform(get("/api/v1/clients/sales/active")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        final var result = objectMapper.readValue(getResult.getResponse().getContentAsString(),
            new TypeReference<List<ClientActiveSaleDto>>() {
            });

        assertEquals(2, result.size());
        assertEquals(sale1.saleId(), result.get(0).saleId());
        assertEquals(sale1.flashSaleItemId(), result.get(0).flashSaleItemId());
        assertEquals(sale1.title(), result.get(0).title());
        assertEquals(sale1.productId(), result.get(0).productId());
        assertEquals(sale1.allocatedStock(), result.get(0).allocatedStock());
        assertEquals(sale1.soldCount(), result.get(0).soldCount());
        assertEquals(0, sale1.salePrice().compareTo(result.get(0).salePrice()));

        assertEquals(sale2.saleId(), result.get(1).saleId());
        assertEquals(sale2.flashSaleItemId(), result.get(1).flashSaleItemId());
        assertEquals(sale2.title(), result.get(1).title());
        assertEquals(sale2.productId(), result.get(1).productId());
        assertEquals(sale2.allocatedStock(), result.get(1).allocatedStock());
        assertEquals(sale2.soldCount(), result.get(1).soldCount());
        assertEquals(0, sale2.salePrice().compareTo(result.get(1).salePrice()));
    }

    @Test
    public void shouldReturnEmptyListWhenNoActiveSales() throws Exception {
        Mockito.when(activeSalesService.getActiveSales()).thenReturn(List.of());

        final var getResult = mockMvc.perform(get("/api/v1/clients/sales/active")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        final var result = objectMapper.readValue(getResult.getResponse().getContentAsString(),
            new TypeReference<List<ClientActiveSaleDto>>() {
            });

        assertEquals(0, result.size());
    }

    @Test
    public void shouldReturnDraftSalesSuccessfully() throws Exception {
        final OffsetDateTime startTime = OffsetDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime endTime = startTime.plusHours(1);

        final ClientDraftSaleDto.DraftSaleProductDto product1 = new ClientDraftSaleDto.DraftSaleProductDto(
            "11111111-1111-1111-1111-111111111111",
            10,
            BigDecimal.valueOf(89.99));

        final ClientDraftSaleDto.DraftSaleProductDto product2 = new ClientDraftSaleDto.DraftSaleProductDto(
            "22222222-2222-2222-2222-222222222222",
            20,
            BigDecimal.valueOf(79.99));

        final ClientDraftSaleDto sale1 = new ClientDraftSaleDto(
            "547cf74d-7b64-44ea-b70f-cbcde09cadc9",
            "Draft Sale 1",
            startTime,
            endTime,
            List.of(product1));

        final ClientDraftSaleDto sale2 = new ClientDraftSaleDto(
            "1c05690e-cd9a-42ee-9f15-194b4c454216",
            "Draft Sale 2",
            startTime.plusDays(1),
            endTime.plusDays(1),
            List.of(product2));

        final List<ClientDraftSaleDto> draftSales = List.of(sale1, sale2);

        Mockito.when(draftSalesService.getDraftSalesWithinDays(7)).thenReturn(draftSales);

        final var getResult = mockMvc.perform(get("/api/v1/clients/sales/draft/7")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        final var result = objectMapper.readValue(getResult.getResponse().getContentAsString(),
            new TypeReference<List<ClientDraftSaleDto>>() {
            });

        assertEquals(2, result.size());
        assertEquals(sale1.saleId(), result.get(0).saleId());
        assertEquals(sale1.title(), result.get(0).title());
        assertEquals(sale1.startTime(), result.get(0).startTime());
        assertEquals(sale1.endTime(), result.get(0).endTime());
        assertEquals(1, result.get(0).products().size());
        assertEquals(product1.productId(), result.get(0).products().get(0).productId());
        assertEquals(product1.allocatedStock(), result.get(0).products().get(0).allocatedStock());
        assertEquals(0, product1.salePrice().compareTo(result.get(0).products().get(0).salePrice()));

        assertEquals(sale2.saleId(), result.get(1).saleId());
        assertEquals(sale2.title(), result.get(1).title());
        assertEquals(sale2.startTime(), result.get(1).startTime());
        assertEquals(sale2.endTime(), result.get(1).endTime());
        assertEquals(1, result.get(1).products().size());
        assertEquals(product2.productId(), result.get(1).products().get(0).productId());
        assertEquals(product2.allocatedStock(), result.get(1).products().get(0).allocatedStock());
        assertEquals(0, product2.salePrice().compareTo(result.get(1).products().get(0).salePrice()));
    }

    @Test
    public void shouldReturnEmptyListWhenNoDraftSales() throws Exception {
        Mockito.when(draftSalesService.getDraftSalesWithinDays(7)).thenReturn(List.of());

        final var getResult = mockMvc.perform(get("/api/v1/clients/sales/draft/7")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        final var result = objectMapper.readValue(getResult.getResponse().getContentAsString(),
            new TypeReference<List<ClientDraftSaleDto>>() {
            });

        assertEquals(0, result.size());
    }

    @Test
    public void shouldReturnBadRequestForNegativeDays() throws Exception {
        mockMvc.perform(get("/api/v1/clients/sales/draft/-1")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void shouldCreateOrderSuccessfully() throws Exception {
        final UUID userId = UUID.randomUUID();
        final UUID flashSaleItemId = UUID.randomUUID();
        final UUID orderId = UUID.randomUUID();

        final CreateOrderDto createOrderDto = new CreateOrderDto(flashSaleItemId, 5);
        final OrderResponseDto orderResponse = new OrderResponseDto(orderId, OrderStatus.PENDING, "Order created");

        Mockito.when(orderService.createOrder(createOrderDto, userId)).thenReturn(orderResponse);

        final String requestBody = objectMapper.writeValueAsString(createOrderDto);

        final var result = mockMvc.perform(post("/api/v1/clients/orders")
            .with(withUser(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andReturn();

        final var response = objectMapper.readValue(result.getResponse().getContentAsString(), OrderResponseDto.class);
        assertEquals(orderId, response.orderId());
        assertEquals(OrderStatus.PENDING, response.status());
    }

    @Test
    public void shouldProcessRefundSuccessfully() throws Exception {
        final UUID userId = UUID.randomUUID();
        final UUID orderId = UUID.randomUUID();

        Mockito.doNothing().when(orderService).handleRefund(orderId);
        Mockito.when(orderService.getOrderById(orderId, userId)).thenReturn(
            new OrderDetailDto(orderId, userId, UUID.randomUUID(), "Product", UUID.randomUUID(), UUID.randomUUID(), "Sale",
                BigDecimal.valueOf(79.99), 5, BigDecimal.valueOf(399.95), OrderStatus.PAID, OffsetDateTime.now()));

        mockMvc.perform(post("/api/v1/clients/orders/" + orderId + "/refund")
            .with(withUser(userId))
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        Mockito.verify(orderService).handleRefund(orderId);
    }

    @Test
    public void shouldReturnBadRequestForInvalidOrderId() throws Exception {
        mockMvc.perform(post("/api/v1/clients/orders/invalid-uuid/refund")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void shouldGetOrderByIdSuccessfully() throws Exception {
        final UUID orderId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        final UUID productId = UUID.randomUUID();
        final UUID flashSaleItemId = UUID.randomUUID();
        final UUID flashSaleId = UUID.randomUUID();
        final OffsetDateTime createdAt = OffsetDateTime.now();

        final OrderDetailDto orderDetail = new OrderDetailDto(
            orderId,
            userId,
            productId,
            "Test Product",
            flashSaleItemId,
            flashSaleId,
            "Test Sale",
            BigDecimal.valueOf(79.99),
            5,
            BigDecimal.valueOf(399.95),
            OrderStatus.PAID,
            createdAt);

        Mockito.when(orderService.getOrderById(orderId, userId)).thenReturn(orderDetail);

        final var result = mockMvc.perform(get("/api/v1/clients/orders/" + orderId)
            .with(withUser(userId))
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        final var response = objectMapper.readValue(result.getResponse().getContentAsString(), OrderDetailDto.class);
        assertEquals(orderId, response.orderId());
        assertEquals(userId, response.userId());
        assertEquals(productId, response.productId());
        assertEquals("Test Product", response.productName());
        assertEquals(flashSaleItemId, response.flashSaleItemId());
        assertEquals(flashSaleId, response.flashSaleId());
        assertEquals("Test Sale", response.flashSaleTitle());
        assertEquals(0, BigDecimal.valueOf(79.99).compareTo(response.soldPrice()));
        assertEquals(5, response.soldQuantity());
        assertEquals(0, BigDecimal.valueOf(399.95).compareTo(response.totalAmount()));
        assertEquals(OrderStatus.PAID, response.status());
    }

    @Test
    public void shouldReturnNotFoundWhenOrderNotFound() throws Exception {
        final UUID orderId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();

        Mockito.when(orderService.getOrderById(orderId, userId))
            .thenThrow(new OrderNotFoundException(orderId));

        mockMvc.perform(get("/api/v1/clients/orders/" + orderId)
            .with(withUser(userId))
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    public void shouldReturnBadRequestForInvalidOrderIdFormat() throws Exception {
        final UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/clients/orders/invalid-uuid")
            .with(withUser(userId))
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void shouldGetOrdersByUserWithoutFilters() throws Exception {
        final UUID userId = UUID.randomUUID();
        final UUID orderId1 = UUID.randomUUID();
        final UUID orderId2 = UUID.randomUUID();
        final OffsetDateTime now = OffsetDateTime.now();

        final OrderDetailDto order1 = new OrderDetailDto(
            orderId1,
            userId,
            UUID.randomUUID(),
            "Product 1",
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Sale 1",
            BigDecimal.valueOf(79.99),
            5,
            BigDecimal.valueOf(399.95),
            OrderStatus.PAID,
            now.minusDays(1));

        final OrderDetailDto order2 = new OrderDetailDto(
            orderId2,
            userId,
            UUID.randomUUID(),
            "Product 2",
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Sale 2",
            BigDecimal.valueOf(89.99),
            3,
            BigDecimal.valueOf(269.97),
            OrderStatus.PENDING,
            now);

        final List<OrderDetailDto> orders = List.of(order2, order1);

        Mockito.when(orderService.getOrdersByUser(userId, null, null, null)).thenReturn(orders);

        final var result = mockMvc.perform(get("/api/v1/clients/orders")
            .with(withUser(userId))
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        final var response = objectMapper.readValue(result.getResponse().getContentAsString(),
            new TypeReference<List<OrderDetailDto>>() {
            });

        assertEquals(2, response.size());
        assertEquals(orderId2, response.get(0).orderId());
        assertEquals(orderId1, response.get(1).orderId());
    }

    @Test
    public void shouldGetOrdersByUserWithStatusFilter() throws Exception {
        final UUID userId = UUID.randomUUID();
        final UUID orderId = UUID.randomUUID();
        final OffsetDateTime now = OffsetDateTime.now();

        final OrderDetailDto order = new OrderDetailDto(
            orderId,
            userId,
            UUID.randomUUID(),
            "Product",
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Sale",
            BigDecimal.valueOf(79.99),
            5,
            BigDecimal.valueOf(399.95),
            OrderStatus.PAID,
            now);

        final List<OrderDetailDto> orders = List.of(order);

        Mockito.when(orderService.getOrdersByUser(userId, OrderStatus.PAID, null, null)).thenReturn(orders);

        final var result = mockMvc.perform(get("/api/v1/clients/orders")
            .with(withUser(userId))
            .param("status", "PAID")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        final var response = objectMapper.readValue(result.getResponse().getContentAsString(),
            new TypeReference<List<OrderDetailDto>>() {
            });

        assertEquals(1, response.size());
        assertEquals(orderId, response.get(0).orderId());
        assertEquals(OrderStatus.PAID, response.get(0).status());
    }

    @Test
    public void shouldGetOrdersByUserWithDateRangeFilter() throws Exception {
        final UUID userId = UUID.randomUUID();
        final UUID orderId = UUID.randomUUID();
        final OffsetDateTime startDate = OffsetDateTime.now().minusDays(7);
        final OffsetDateTime endDate = OffsetDateTime.now();
        final OffsetDateTime orderDate = OffsetDateTime.now().minusDays(3);

        final OrderDetailDto order = new OrderDetailDto(
            orderId,
            userId,
            UUID.randomUUID(),
            "Product",
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Sale",
            BigDecimal.valueOf(79.99),
            5,
            BigDecimal.valueOf(399.95),
            OrderStatus.PAID,
            orderDate);

        final List<OrderDetailDto> orders = List.of(order);

        Mockito.when(orderService.getOrdersByUser(userId, null, startDate, endDate)).thenReturn(orders);

        final var result = mockMvc.perform(get("/api/v1/clients/orders")
            .with(withUser(userId))
            .param("startDate", startDate.toString())
            .param("endDate", endDate.toString())
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        final var response = objectMapper.readValue(result.getResponse().getContentAsString(),
            new TypeReference<List<OrderDetailDto>>() {
            });

        assertEquals(1, response.size());
        assertEquals(orderId, response.get(0).orderId());
    }

    @Test
    public void shouldGetOrdersByUserWithAllFilters() throws Exception {
        final UUID userId = UUID.randomUUID();
        final UUID orderId = UUID.randomUUID();
        final OffsetDateTime startDate = OffsetDateTime.now().minusDays(7);
        final OffsetDateTime endDate = OffsetDateTime.now();
        final OffsetDateTime orderDate = OffsetDateTime.now().minusDays(3);

        final OrderDetailDto order = new OrderDetailDto(
            orderId,
            userId,
            UUID.randomUUID(),
            "Product",
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Sale",
            BigDecimal.valueOf(79.99),
            5,
            BigDecimal.valueOf(399.95),
            OrderStatus.PAID,
            orderDate);

        final List<OrderDetailDto> orders = List.of(order);

        Mockito.when(orderService.getOrdersByUser(userId, OrderStatus.PAID, startDate, endDate)).thenReturn(orders);

        final var result = mockMvc.perform(get("/api/v1/clients/orders")
            .with(withUser(userId))
            .param("status", "PAID")
            .param("startDate", startDate.toString())
            .param("endDate", endDate.toString())
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        final var response = objectMapper.readValue(result.getResponse().getContentAsString(),
            new TypeReference<List<OrderDetailDto>>() {
            });

        assertEquals(1, response.size());
        assertEquals(orderId, response.get(0).orderId());
        assertEquals(OrderStatus.PAID, response.get(0).status());
    }

    @Test
    public void shouldReturnBadRequestForInvalidStatus() throws Exception {
        final UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/clients/orders")
            .with(withUser(userId))
            .param("status", "INVALID_STATUS")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void shouldReturnBadRequestForInvalidDateRange() throws Exception {
        final UUID userId = UUID.randomUUID();
        final OffsetDateTime startDate = OffsetDateTime.now();
        final OffsetDateTime endDate = OffsetDateTime.now().minusDays(1); // endDate before startDate

        Mockito.when(orderService.getOrdersByUser(userId, null, startDate, endDate))
            .thenThrow(new IllegalArgumentException("Start date must be before or equal to end date"));

        mockMvc.perform(get("/api/v1/clients/orders")
            .with(withUser(userId))
            .param("startDate", startDate.toString())
            .param("endDate", endDate.toString())
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void shouldReturnEmptyListWhenNoOrdersFound() throws Exception {
        final UUID userId = UUID.randomUUID();

        Mockito.when(orderService.getOrdersByUser(userId, null, null, null)).thenReturn(List.of());

        final var result = mockMvc.perform(get("/api/v1/clients/orders")
            .with(withUser(userId))
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        final var response = objectMapper.readValue(result.getResponse().getContentAsString(),
            new TypeReference<List<OrderDetailDto>>() {
            });

        assertEquals(0, response.size());
    }

    @Test
    public void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/clients/orders")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andReturn();
    }

}
