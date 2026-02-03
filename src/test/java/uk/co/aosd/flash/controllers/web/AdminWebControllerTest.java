package uk.co.aosd.flash.controllers.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
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
import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.FlashSaleResponseDto;
import uk.co.aosd.flash.dto.FlashSaleItemDto;
import uk.co.aosd.flash.dto.OrderDetailDto;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.exc.FlashSaleItemNotFoundException;
import uk.co.aosd.flash.exc.FlashSaleNotFoundException;
import uk.co.aosd.flash.exc.InsufficientResourcesException;
import uk.co.aosd.flash.exc.InvalidOrderStatusException;
import uk.co.aosd.flash.exc.ProductNotFoundException;
import uk.co.aosd.flash.services.AnalyticsService;
import uk.co.aosd.flash.services.FlashSalesService;
import uk.co.aosd.flash.services.OrderService;
import uk.co.aosd.flash.services.ProductsService;

@WebMvcTest(controllers = AdminWebController.class)
@Import({ TestSecurityConfig.class, uk.co.aosd.flash.config.WebMvcConfig.class, uk.co.aosd.flash.errorhandling.ErrorMapper.class })
@ActiveProfiles("test")
class AdminWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductsService productsService;

    @MockitoBean
    private FlashSalesService flashSalesService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private AnalyticsService analyticsService;

    @Test
    void adminIndex_returnsAdminIndexView() throws Exception {
        mockMvc.perform(get("/admin").with(user("admin").roles("ADMIN_USER")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/index"));
    }

    @Test
    void listProducts_returnsProductsList() throws Exception {
        when(productsService.getAllProducts(any(), any(), any())).thenReturn(List.of(
            new ProductDto("id1", "Product 1", "Desc", 10, BigDecimal.TEN, 0)));

        mockMvc.perform(get("/admin/products").with(user("admin").roles("ADMIN_USER")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/products/list"))
            .andExpect(model().attribute("products", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void newProduct_returnsNewProductFormWithProductDto() throws Exception {
        mockMvc.perform(get("/admin/products/new").with(user("admin").roles("ADMIN_USER")).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/products/new"))
            .andExpect(model().attributeExists("productDto"));
    }

    @Test
    void createProduct_withValidDto_redirectsToProductsList() throws Exception {
        mockMvc.perform(post("/admin/products")
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf())
                .param("id", "")
                .param("name", "New Product")
                .param("description", "Description")
                .param("totalPhysicalStock", "100")
                .param("basePrice", "29.99")
                .param("reservedCount", "0"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/products"))
            .andExpect(flash().attribute("success", "Product created successfully"));

        verify(productsService).createProduct(any(ProductDto.class));
    }

    @Test
    void createProduct_withBindingErrors_redirectsToNew() throws Exception {
        mockMvc.perform(post("/admin/products")
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf())
                .param("id", "")
                .param("name", "")  // empty name fails validation
                .param("description", "")
                .param("totalPhysicalStock", "-1")
                .param("basePrice", "29.99")
                .param("reservedCount", "0"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/products/new"))
            .andExpect(flash().attributeExists("productDto"));

        verify(productsService, never()).createProduct(any());
    }

    @Test
    void createProduct_whenException_redirectsWithError() throws Exception {
        when(productsService.createProduct(any())).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/admin/products")
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf())
                .param("id", "")
                .param("name", "New Product")
                .param("description", "Description")
                .param("totalPhysicalStock", "100")
                .param("basePrice", "29.99")
                .param("reservedCount", "0"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/products/new"))
            .andExpect(flash().attribute("error", org.hamcrest.Matchers.containsString("Failed to create product")));
    }

    @Test
    void viewProduct_whenFound_returnsDetailView() throws Exception {
        final var product = new ProductDto("id1", "Product 1", "Desc", 10, BigDecimal.TEN, 0);
        when(productsService.getProductById("id1")).thenReturn(Optional.of(product));

        mockMvc.perform(get("/admin/products/id1").with(user("admin").roles("ADMIN_USER")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/products/detail"))
            .andExpect(model().attribute("product", product));
    }

    @Test
    void viewProduct_whenNotFound_redirectsToList() throws Exception {
        when(productsService.getProductById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/products/missing").with(user("admin").roles("ADMIN_USER")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/products?error=notfound"));
    }

    @Test
    void listSales_returnsSalesList() throws Exception {
        final var now = OffsetDateTime.now(ZoneOffset.UTC);
        when(flashSalesService.getAllFlashSales(eq(null), eq(null), eq(null), eq(null)))
            .thenReturn(List.of(new FlashSaleResponseDto("sale-1", "Sale", now, now.plusHours(1),
                SaleStatus.DRAFT, Collections.emptyList())));

        mockMvc.perform(get("/admin/sales").with(user("admin").roles("ADMIN_USER")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/sales/list"))
            .andExpect(model().attributeExists("sales"));
    }

    @Test
    void newSale_returnsNewSaleForm() throws Exception {
        when(productsService.getAllProducts()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/sales/new").with(user("admin").roles("ADMIN_USER")).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/sales/new"))
            .andExpect(model().attributeExists("createSaleDto"))
            .andExpect(model().attributeExists("products"));
    }

    @Test
    void createSale_withDatetimeLocalFormatAndNoProducts_redirectsToSaleDetail() throws Exception {
        final UUID saleId = UUID.randomUUID();
        when(flashSalesService.createFlashSale(any())).thenReturn(saleId);
        when(productsService.getAllProducts()).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/admin/sales")
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf())
                .param("id", "")
                .param("title", "New Flash Sale")
                .param("startTime", "2026-02-01T14:30")
                .param("endTime", "2026-02-01T16:00")
                .param("status", "DRAFT"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/sales/" + saleId))
            .andExpect(flash().attribute("success", "Flash sale created successfully"));

        verify(flashSalesService).createFlashSale(any());
    }

    @Test
    void viewSale_whenValidId_returnsDetailView() throws Exception {
        final var saleId = UUID.randomUUID();
        final var now = OffsetDateTime.now(ZoneOffset.UTC);
        final var sale = new FlashSaleResponseDto(saleId.toString(), "Sale", now, now.plusHours(1),
            SaleStatus.ACTIVE, List.of(new FlashSaleItemDto(UUID.randomUUID().toString(), "product-1", "Product", 10, 0, BigDecimal.TEN)));
        when(flashSalesService.getFlashSaleById(saleId)).thenReturn(sale);
        when(productsService.getAllProducts()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/sales/" + saleId).with(user("admin").roles("ADMIN_USER")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/sales/detail"))
            .andExpect(model().attribute("sale", sale))
            .andExpect(content().string(containsString("ACTIVE")))
            .andExpect(model().attributeExists("addFlashSaleItemDto"));
    }

    @Test
    void viewSale_whenInvalidId_redirectsToList() throws Exception {
        mockMvc.perform(get("/admin/sales/not-a-uuid").with(user("admin").roles("ADMIN_USER")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/sales?error=notfound"));
    }

    @Test
    void postAddItem_success_redirectsWithSuccess() throws Exception {
        final var saleId = UUID.randomUUID();
        final var productId = UUID.randomUUID().toString();
        final var now = OffsetDateTime.now(ZoneOffset.UTC);
        final var updatedSale = new FlashSaleResponseDto(saleId.toString(), "Sale", now, now.plusHours(1),
            SaleStatus.DRAFT, Collections.emptyList());
        when(flashSalesService.addItemsToFlashSale(eq(saleId), any())).thenReturn(updatedSale);

        mockMvc.perform(post("/admin/sales/" + saleId + "/items")
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf())
                .param("productId", productId)
                .param("allocatedStock", "10")
                .param("salePrice", "19.99"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/sales/" + saleId))
            .andExpect(flash().attribute("success", "Flash sale item added successfully"));

        verify(flashSalesService, times(1)).addItemsToFlashSale(eq(saleId), any());
    }

    @Test
    void postAddItem_whenNotDraft_redirectsWithError() throws Exception {
        final var saleId = UUID.randomUUID();
        final var productId = UUID.randomUUID().toString();
        when(flashSalesService.addItemsToFlashSale(eq(saleId), any()))
            .thenThrow(new IllegalArgumentException("Only DRAFT flash sales can have items added"));

        mockMvc.perform(post("/admin/sales/" + saleId + "/items")
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf())
                .param("productId", productId)
                .param("allocatedStock", "10")
                .param("salePrice", "19.99"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/sales/" + saleId))
            .andExpect(flash().attributeExists("error"));

        verify(flashSalesService, times(1)).addItemsToFlashSale(eq(saleId), any());
    }

    @Test
    void postAddItem_invalidProduct_redirectsWithError() throws Exception {
        final var saleId = UUID.randomUUID();
        final var productId = UUID.randomUUID().toString();
        when(flashSalesService.addItemsToFlashSale(eq(saleId), any()))
            .thenThrow(new ProductNotFoundException(productId));

        mockMvc.perform(post("/admin/sales/" + saleId + "/items")
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf())
                .param("productId", productId)
                .param("allocatedStock", "10")
                .param("salePrice", "19.99"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/sales/" + saleId))
            .andExpect(flash().attributeExists("error"));
    }

    @Test
    void postAddItem_validationError_redirectsWithErrors() throws Exception {
        final var saleId = UUID.randomUUID();

        mockMvc.perform(post("/admin/sales/" + saleId + "/items")
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf())
                .param("productId", "")
                .param("allocatedStock", "-1")
                .param("salePrice", "19.99"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/sales/" + saleId))
            .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.addFlashSaleItemDto"));

        verify(flashSalesService, never()).addItemsToFlashSale(any(), any());
    }

    @Test
    void postUpdateItem_success_redirectsWithSuccess() throws Exception {
        final var saleId = UUID.randomUUID();
        final var itemId = UUID.randomUUID();
        final var now = OffsetDateTime.now(ZoneOffset.UTC);
        final var updatedSale = new FlashSaleResponseDto(saleId.toString(), "Sale", now, now.plusHours(1),
            SaleStatus.DRAFT, Collections.emptyList());
        when(flashSalesService.updateFlashSaleItem(eq(saleId), eq(itemId), any())).thenReturn(updatedSale);

        mockMvc.perform(post("/admin/sales/" + saleId + "/items/" + itemId)
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf())
                .param("allocatedStock", "20")
                .param("salePrice", "14.99"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/sales/" + saleId))
            .andExpect(flash().attribute("success", "Flash sale item updated successfully"));

        verify(flashSalesService, times(1)).updateFlashSaleItem(eq(saleId), eq(itemId), any());
    }

    @Test
    void postUpdateItem_whenNotDraft_redirectsWithError() throws Exception {
        final var saleId = UUID.randomUUID();
        final var itemId = UUID.randomUUID();
        when(flashSalesService.updateFlashSaleItem(eq(saleId), eq(itemId), any()))
            .thenThrow(new IllegalArgumentException("Only DRAFT flash sales can have items updated"));

        mockMvc.perform(post("/admin/sales/" + saleId + "/items/" + itemId)
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf())
                .param("allocatedStock", "20")
                .param("salePrice", "14.99"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/sales/" + saleId))
            .andExpect(flash().attributeExists("error"));
    }

    @Test
    void postUpdateItem_itemNotFound_redirectsWithError() throws Exception {
        final var saleId = UUID.randomUUID();
        final var itemId = UUID.randomUUID();
        when(flashSalesService.updateFlashSaleItem(eq(saleId), eq(itemId), any()))
            .thenThrow(new FlashSaleItemNotFoundException(itemId));

        mockMvc.perform(post("/admin/sales/" + saleId + "/items/" + itemId)
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf())
                .param("allocatedStock", "20")
                .param("salePrice", "14.99"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/sales/" + saleId))
            .andExpect(flash().attributeExists("error"));
    }

    @Test
    void postUpdateItem_bothParamsNull_redirectsWithError() throws Exception {
        final var saleId = UUID.randomUUID();
        final var itemId = UUID.randomUUID();

        mockMvc.perform(post("/admin/sales/" + saleId + "/items/" + itemId)
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/sales/" + saleId))
            .andExpect(flash().attribute("error", "Provide at least one of allocated stock or sale price"));

        verify(flashSalesService, never()).updateFlashSaleItem(any(), any(), any());
    }

    @Test
    void postDeleteItem_success_rendersDetailWithUpdatedSale() throws Exception {
        final var saleId = UUID.randomUUID();
        final var itemId = UUID.randomUUID();
        final var now = OffsetDateTime.now(ZoneOffset.UTC);
        final var updatedSale = new FlashSaleResponseDto(saleId.toString(), "Sale", now, now.plusHours(1),
            SaleStatus.DRAFT, Collections.emptyList());
        when(flashSalesService.removeFlashSaleItem(eq(saleId), eq(itemId))).thenReturn(updatedSale);
        when(productsService.getAllProducts()).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/admin/sales/" + saleId + "/items/" + itemId + "/delete")
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/sales/detail"))
            .andExpect(model().attribute("sale", updatedSale))
            .andExpect(model().attribute("success", "Flash sale item removed successfully"));

        verify(flashSalesService, times(1)).removeFlashSaleItem(eq(saleId), eq(itemId));
    }

    @Test
    void postDeleteItem_whenNotDraft_redirectsWithError() throws Exception {
        final var saleId = UUID.randomUUID();
        final var itemId = UUID.randomUUID();
        when(flashSalesService.removeFlashSaleItem(eq(saleId), eq(itemId)))
            .thenThrow(new IllegalArgumentException("Only DRAFT flash sales can have items removed"));

        mockMvc.perform(post("/admin/sales/" + saleId + "/items/" + itemId + "/delete")
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/sales/" + saleId))
            .andExpect(flash().attributeExists("error"));
    }

    @Test
    void postDeleteItem_itemNotFound_redirectsWithError() throws Exception {
        final var saleId = UUID.randomUUID();
        final var itemId = UUID.randomUUID();
        when(flashSalesService.removeFlashSaleItem(eq(saleId), eq(itemId)))
            .thenThrow(new FlashSaleItemNotFoundException(itemId));

        mockMvc.perform(post("/admin/sales/" + saleId + "/items/" + itemId + "/delete")
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/sales/" + saleId))
            .andExpect(flash().attributeExists("error"));
    }

    @Test
    void listOrders_returnsOrdersList() throws Exception {
        when(orderService.getAllOrders(eq(null), eq(null), eq(null), eq(null)))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/orders").with(user("admin").roles("ADMIN_USER")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/orders/list"))
            .andExpect(model().attributeExists("orders"));
    }

    @Test
    void viewOrder_whenValidId_returnsDetailView() throws Exception {
        final var orderId = UUID.randomUUID();
        final var order = new OrderDetailDto(orderId, UUID.randomUUID(), UUID.randomUUID(), "Product",
            UUID.randomUUID(), UUID.randomUUID(), "Sale", BigDecimal.TEN, 1, BigDecimal.TEN, OrderStatus.PENDING,
            OffsetDateTime.now(ZoneOffset.UTC), List.of());
        when(orderService.getOrderByIdForAdmin(orderId)).thenReturn(order);

        mockMvc.perform(get("/admin/orders/" + orderId).with(user("admin").roles("ADMIN_USER")).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/orders/detail"))
            .andExpect(model().attribute("order", order));
    }

    @Test
    void viewOrder_whenInvalidId_redirectsToList() throws Exception {
        mockMvc.perform(get("/admin/orders/not-a-uuid").with(user("admin").roles("ADMIN_USER")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/orders?error=notfound"));
    }

    @Test
    void updateOrderStatus_withValidStatus_redirectsWithSuccess() throws Exception {
        final var orderId = UUID.randomUUID();

        mockMvc.perform(post("/admin/orders/" + orderId + "/status")
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf())
                .param("status", "PAID"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/orders/" + orderId))
            .andExpect(flash().attribute("success", "Order status updated successfully"));
    }

    @Test
    void updateOrderStatus_whenInvalidOrderStatusException_redirectsWithError() throws Exception {
        final var orderId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new InvalidOrderStatusException(orderId, OrderStatus.PENDING, OrderStatus.PAID, "dispatch"))
            .when(orderService).updateOrderStatus(eq(orderId), eq(OrderStatus.DISPATCHED));

        mockMvc.perform(post("/admin/orders/" + orderId + "/status")
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf())
                .param("status", "DISPATCHED"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/orders/" + orderId))
            .andExpect(flash().attributeExists("error"));
    }

    @Test
    void updateOrderStatus_whenGenericException_redirectsWithError() throws Exception {
        final var orderId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new RuntimeException("DB error"))
            .when(orderService).updateOrderStatus(eq(orderId), any());

        mockMvc.perform(post("/admin/orders/" + orderId + "/status")
                .with(user("admin").roles("ADMIN_USER"))
                .with(csrf())
                .param("status", "PAID"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/orders/" + orderId))
            .andExpect(flash().attribute("error", org.hamcrest.Matchers.containsString("Failed to update order status")));
    }

    @Test
    void analytics_returnsDashboardView() throws Exception {
        when(analyticsService.getSalesMetrics(any(), any())).thenReturn(null);
        when(analyticsService.getRevenueMetrics(any(), any())).thenReturn(null);
        when(analyticsService.getOrderStatistics(any(), any())).thenReturn(null);
        when(analyticsService.getProductPerformance()).thenReturn(null);

        mockMvc.perform(get("/admin/analytics").with(user("admin").roles("ADMIN_USER")).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/analytics/dashboard"));
    }
}
