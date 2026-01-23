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

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.UUID;

import uk.co.aosd.flash.domain.OrderStatus;
import uk.co.aosd.flash.dto.ClientActiveSaleDto;
import uk.co.aosd.flash.dto.ClientDraftSaleDto;
import uk.co.aosd.flash.dto.ClientProductDto;
import uk.co.aosd.flash.dto.CreateOrderDto;
import uk.co.aosd.flash.dto.OrderResponseDto;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;
import uk.co.aosd.flash.services.ActiveSalesService;
import uk.co.aosd.flash.services.DraftSalesService;
import uk.co.aosd.flash.services.OrderService;
import uk.co.aosd.flash.services.ProductsService;

/**
 * Admin API Web Test.
 */
@WebMvcTest(ClientRestApi.class)
@Import({ ErrorMapper.class, GlobalExceptionHandler.class })
public class ClientRestApiTest {

    @Autowired
    private MockMvc mockMvc;

    private static ObjectMapper objectMapper = new ObjectMapper();

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
            "Sale 1",
            startTime,
            endTime,
            "11111111-1111-1111-1111-111111111111",
            10,
            5,
            BigDecimal.valueOf(89.99));

        final ClientActiveSaleDto sale2 = new ClientActiveSaleDto(
            "1c05690e-cd9a-42ee-9f15-194b4c454216",
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
        assertEquals(sale1.title(), result.get(0).title());
        assertEquals(sale1.productId(), result.get(0).productId());
        assertEquals(sale1.allocatedStock(), result.get(0).allocatedStock());
        assertEquals(sale1.soldCount(), result.get(0).soldCount());
        assertEquals(0, sale1.salePrice().compareTo(result.get(0).salePrice()));

        assertEquals(sale2.saleId(), result.get(1).saleId());
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

        final CreateOrderDto createOrderDto = new CreateOrderDto(userId, flashSaleItemId, 5);
        final OrderResponseDto orderResponse = new OrderResponseDto(orderId, OrderStatus.PENDING, "Order created");

        Mockito.when(orderService.createOrder(createOrderDto)).thenReturn(orderResponse);

        final String requestBody = objectMapper.writeValueAsString(createOrderDto);

        final var result = mockMvc.perform(post("/api/v1/clients/orders")
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
        final UUID orderId = UUID.randomUUID();

        Mockito.doNothing().when(orderService).handleRefund(orderId);

        mockMvc.perform(post("/api/v1/clients/orders/" + orderId + "/refund")
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

}
