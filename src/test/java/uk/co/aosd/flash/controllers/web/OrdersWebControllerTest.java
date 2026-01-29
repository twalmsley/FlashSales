package uk.co.aosd.flash.controllers.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
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
import uk.co.aosd.flash.dto.OrderDetailDto;
import uk.co.aosd.flash.security.CustomUserDetailsService;
import uk.co.aosd.flash.services.OrderService;

@WebMvcTest(controllers = OrdersWebController.class)
@Import({ TestSecurityConfig.class, uk.co.aosd.flash.errorhandling.ErrorMapper.class })
@ActiveProfiles("test")
class OrdersWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void listOrders_returnsOrdersList() throws Exception {
        final var userId = UUID.randomUUID();
        when(userDetailsService.getUserIdByUsername("user")).thenReturn(userId);
        when(orderService.getOrdersByUser(eq(userId), eq(null), eq(null), eq(null)))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/orders").with(user("user").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(view().name("orders/list"))
            .andExpect(model().attribute("orders", Collections.emptyList()));
    }

    @Test
    void orderDetail_whenValidUuid_returnsDetailView() throws Exception {
        final var userId = UUID.randomUUID();
        final var orderId = UUID.randomUUID();
        when(userDetailsService.getUserIdByUsername("user")).thenReturn(userId);
        final var order = new OrderDetailDto(orderId, userId, UUID.randomUUID(), "Product",
            UUID.randomUUID(), UUID.randomUUID(), "Sale", BigDecimal.TEN, 1, BigDecimal.TEN, OrderStatus.PENDING,
            OffsetDateTime.now(ZoneOffset.UTC));
        when(orderService.getOrderById(eq(orderId), eq(userId))).thenReturn(order);

        mockMvc.perform(get("/orders/" + orderId).with(user("user").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(view().name("orders/detail"))
            .andExpect(model().attribute("order", order));
    }

    @Test
    void orderDetail_whenInvalidUuid_redirectsToList() throws Exception {
        mockMvc.perform(get("/orders/not-a-uuid").with(user("user").roles("USER")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/orders?error=notfound"));
    }

    @Test
    void orderDetail_whenOrderNotFound_redirectsToList() throws Exception {
        final var userId = UUID.randomUUID();
        final var orderId = UUID.randomUUID();
        when(userDetailsService.getUserIdByUsername("user")).thenReturn(userId);
        when(orderService.getOrderById(eq(orderId), eq(userId)))
            .thenThrow(new uk.co.aosd.flash.exc.OrderNotFoundException(orderId));

        mockMvc.perform(get("/orders/" + orderId).with(user("user").roles("USER")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/orders?error=notfound"));
    }
}
