package uk.co.aosd.flash.controllers.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.co.aosd.flash.config.TestSecurityConfig;
import uk.co.aosd.flash.dto.ClientActiveSaleDto;
import uk.co.aosd.flash.services.ActiveSalesService;

@WebMvcTest(controllers = HomeController.class)
@Import({ TestSecurityConfig.class, uk.co.aosd.flash.errorhandling.ErrorMapper.class })
@ActiveProfiles("test")
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActiveSalesService activeSalesService;

    @Test
    void home_withAnonymous_returnsHomeViewAndCacheControlHeader() throws Exception {
        when(activeSalesService.getActiveSales()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/").with(anonymous()))
            .andExpect(status().isOk())
            .andExpect(view().name("home"))
            .andExpect(model().attributeExists("activeSales"))
            .andExpect(model().attribute("isAuthenticated", false))
            .andExpect(model().attribute("showAdminLinks", false))
            .andExpect(header().string("Cache-Control", "private, no-store"));
    }

    @Test
    void home_withAuthenticatedUser_showsAuthenticatedButNoAdminLinks() throws Exception {
        when(activeSalesService.getActiveSales()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/").with(user("user").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(view().name("home"))
            .andExpect(model().attribute("isAuthenticated", true))
            .andExpect(model().attribute("showAdminLinks", false))
            .andExpect(header().string("Cache-Control", "private, no-store"));
    }

    @Test
    void home_withAdminUser_showsAdminLinks() throws Exception {
        final var now = OffsetDateTime.now(ZoneOffset.UTC);
        when(activeSalesService.getActiveSales()).thenReturn(
            java.util.List.of(new ClientActiveSaleDto("sale-1", "item-1", "Product",
                now, now.plusHours(1), "product-1", "Product", "desc", BigDecimal.ZERO, 10, 1, BigDecimal.ZERO)));

        mockMvc.perform(get("/").with(user("admin").roles("ADMIN_USER")))
            .andExpect(status().isOk())
            .andExpect(view().name("home"))
            .andExpect(model().attribute("isAuthenticated", true))
            .andExpect(model().attribute("showAdminLinks", true))
            .andExpect(model().attribute("activeSales", org.hamcrest.Matchers.hasSize(1)));
    }
}
