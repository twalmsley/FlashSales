package uk.co.aosd.flash.controllers.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.co.aosd.flash.config.TestSecurityConfig;
import uk.co.aosd.flash.domain.OrderStatus;
import uk.co.aosd.flash.dto.ClientActiveSaleDto;
import uk.co.aosd.flash.dto.CreateOrderDto;
import uk.co.aosd.flash.dto.OrderResponseDto;
import uk.co.aosd.flash.security.CustomUserDetailsService;
import uk.co.aosd.flash.services.ActiveSalesService;
import uk.co.aosd.flash.services.OrderMessageSender;
import uk.co.aosd.flash.services.OrderService;
import uk.co.aosd.flash.services.ProductsService;

@WebMvcTest(controllers = ClientWebController.class)
@Import({ TestSecurityConfig.class, uk.co.aosd.flash.errorhandling.ErrorMapper.class })
@ActiveProfiles("test")
class ClientWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActiveSalesService activeSalesService;

    @MockitoBean
    private ProductsService productsService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderMessageSender orderMessageSender;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private static final String ITEM_ID = "b1b7a3c0-8d3b-4d10-8cc1-3c5f88f4bb5a";

    private static ClientActiveSaleDto activeSale(final String itemId) {
        final var now = OffsetDateTime.now(ZoneOffset.UTC);
        return new ClientActiveSaleDto("sale-1", itemId, "Sale", now, now.plusHours(1),
            "product-1", "Product", "Desc", BigDecimal.ZERO, 10, 0, BigDecimal.TEN);
    }

    @Test
    void listSales_returnsSalesList() throws Exception {
        final var sale = activeSale("item-1");
        when(activeSalesService.getActiveSales()).thenReturn(List.of(sale));

        mockMvc.perform(get("/sales").with(user("user").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(view().name("sales/list"))
            .andExpect(model().attribute("activeSales", List.of(sale)));
    }

    @Test
    void saleDetail_whenFound_returnsDetailView() throws Exception {
        final var sale = activeSale(ITEM_ID);
        when(activeSalesService.getActiveSales()).thenReturn(List.of(sale));
        when(orderService.findOrderByUserAndFlashSaleItem(any(UUID.class), eq(UUID.fromString(ITEM_ID))))
            .thenReturn(Optional.empty());

        mockMvc.perform(get("/sales/" + ITEM_ID).with(user("user").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(view().name("sales/detail"))
            .andExpect(model().attribute("sale", sale))
            .andExpect(model().attributeExists("createOrderDto"));
    }

    @Test
    void saleDetail_whenNotFound_redirectsToList() throws Exception {
        when(activeSalesService.getActiveSales()).thenReturn(List.of(activeSale(ITEM_ID)));

        mockMvc.perform(get("/sales/00000000-0000-0000-0000-000000000000").with(user("user").roles("USER")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/sales?error=notfound"));
    }

    @Test
    void createOrder_withValidDto_redirectsToOrdersWithSuccess() throws Exception {
        final var userId = UUID.randomUUID();
        final var orderId = UUID.randomUUID();
        when(userDetailsService.getUserIdByUsername("user")).thenReturn(userId);
        when(orderService.createOrder(any(CreateOrderDto.class), eq(userId)))
            .thenReturn(new OrderResponseDto(orderId, OrderStatus.PENDING, null));

        mockMvc.perform(post("/sales/" + ITEM_ID + "/order")
                .with(user("user").roles("USER"))
                .with(csrf())
                .param("flashSaleItemId", ITEM_ID)
                .param("quantity", "2"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/orders"))
            .andExpect(flash().attribute("success", org.hamcrest.Matchers.containsString("Order created successfully")));

        verify(orderMessageSender).sendForProcessing(orderId);
    }

    @Test
    void createOrder_withBindingErrors_redirectsBackToSale() throws Exception {
        mockMvc.perform(post("/sales/" + ITEM_ID + "/order")
                .with(user("user").roles("USER"))
                .with(csrf())
                .param("flashSaleItemId", ITEM_ID)
                .param("quantity", "0"))  // invalid: must be positive
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/sales/" + ITEM_ID))
            .andExpect(flash().attributeExists("createOrderDto"));

        verify(orderService, never()).createOrder(any(), any());
    }

    @Test
    void createOrder_whenException_redirectsWithError() throws Exception {
        when(userDetailsService.getUserIdByUsername("user")).thenReturn(UUID.randomUUID());
        when(orderService.createOrder(any(), any())).thenThrow(new RuntimeException("Stock error"));

        mockMvc.perform(post("/sales/" + ITEM_ID + "/order")
                .with(user("user").roles("USER"))
                .with(csrf())
                .param("flashSaleItemId", ITEM_ID)
                .param("quantity", "1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/sales/" + ITEM_ID))
            .andExpect(flash().attribute("error", org.hamcrest.Matchers.containsString("Failed to create order")));
    }
}
