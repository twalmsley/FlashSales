package uk.co.aosd.flash.controllers.web;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.AddFlashSaleItemDto;
import uk.co.aosd.flash.dto.CreateSaleDto;
import uk.co.aosd.flash.dto.FlashSaleResponseDto;
import uk.co.aosd.flash.dto.OrderDetailDto;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.dto.UpdateFlashSaleItemDto;
import uk.co.aosd.flash.dto.UpdateOrderStatusDto;
import uk.co.aosd.flash.exc.FlashSaleItemNotFoundException;
import uk.co.aosd.flash.exc.FlashSaleNotFoundException;
import uk.co.aosd.flash.exc.InsufficientResourcesException;
import uk.co.aosd.flash.exc.InvalidOrderStatusException;
import uk.co.aosd.flash.exc.ProductNotFoundException;
import uk.co.aosd.flash.services.AnalyticsService;
import uk.co.aosd.flash.services.FlashSalesService;
import uk.co.aosd.flash.services.OrderService;
import uk.co.aosd.flash.services.ProductsService;

/**
 * Web controller for admin management pages.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminWebController {

    private static final Logger log = LoggerFactory.getLogger(AdminWebController.class);

    private final ProductsService productsService;
    private final FlashSalesService flashSalesService;
    private final OrderService orderService;
    private final AnalyticsService analyticsService;

    // Admin Index
    @GetMapping({ "", "/" })
    public String adminIndex() {
        return "admin/index";
    }

    // Products Management
    @GetMapping("/products")
    public String listProducts(final Model model) {
        model.addAttribute("products", productsService.getAllProducts());
        return "admin/products/list";
    }

    @GetMapping("/products/new")
    public String newProduct(final Model model) {
        if (!model.containsAttribute("productDto")) {
            model.addAttribute("productDto", new ProductDto("", "", "", 0, java.math.BigDecimal.ZERO, 0));
        }
        return "admin/products/new";
    }

    @PostMapping("/products")
    public String createProduct(
        @Valid @ModelAttribute final ProductDto productDto,
        final BindingResult bindingResult,
        final RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productDto", bindingResult);
            redirectAttributes.addFlashAttribute("productDto", productDto);
            return "redirect:/admin/products/new";
        }

        try {
            productsService.createProduct(productDto);
            redirectAttributes.addFlashAttribute("success", "Product created successfully");
            return "redirect:/admin/products";
        } catch (final Exception e) {
            log.error("Error creating product", e);
            redirectAttributes.addFlashAttribute("error", "Failed to create product: " + e.getMessage());
            redirectAttributes.addFlashAttribute("productDto", productDto);
            return "redirect:/admin/products/new";
        }
    }

    @GetMapping("/products/{id}")
    public String viewProduct(@PathVariable final String id, final Model model) {
        final var product = productsService.getProductById(id);
        if (product.isEmpty()) {
            return "redirect:/admin/products?error=notfound";
        }
        model.addAttribute("product", product.get());
        return "admin/products/detail";
    }

    // Flash Sales Management
    @GetMapping("/sales")
    public String listSales(
        @RequestParam(required = false) final String status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime endDate,
        final Model model) {

        final SaleStatus saleStatus = status != null ? SaleStatus.valueOf(status) : null;
        final List<FlashSaleResponseDto> sales = flashSalesService.getAllFlashSales(saleStatus, startDate, endDate);
        model.addAttribute("sales", sales);
        model.addAttribute("statusFilter", status);
        return "admin/sales/list";
    }

    @GetMapping("/sales/new")
    public String newSale(final Model model) {
        if (!model.containsAttribute("createSaleDto")) {
            model.addAttribute("createSaleDto", new CreateSaleDto("", "", OffsetDateTime.now().plusHours(1), OffsetDateTime.now().plusHours(2),
                SaleStatus.DRAFT, java.util.Collections.emptyList()));
        }
        model.addAttribute("products", productsService.getAllProducts());
        return "admin/sales/new";
    }

    @PostMapping("/sales")
    public String createSale(
        @Valid @ModelAttribute final CreateSaleDto createSaleDto,
        final BindingResult bindingResult,
        final RedirectAttributes redirectAttributes) {

        final CreateSaleDto dto = createSaleDto.products() != null
            ? createSaleDto
            : new CreateSaleDto(createSaleDto.id(), createSaleDto.title(), createSaleDto.startTime(),
                createSaleDto.endTime(), createSaleDto.status(), Collections.emptyList());

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.createSaleDto", bindingResult);
            redirectAttributes.addFlashAttribute("createSaleDto", dto);
            return "redirect:/admin/sales/new";
        }

        try {
            final UUID saleId = flashSalesService.createFlashSale(dto);
            redirectAttributes.addFlashAttribute("success", "Flash sale created successfully");
            return "redirect:/admin/sales/" + saleId;
        } catch (final Exception e) {
            log.error("Error creating flash sale", e);
            redirectAttributes.addFlashAttribute("error", "Failed to create flash sale: " + e.getMessage());
            redirectAttributes.addFlashAttribute("createSaleDto", dto);
            return "redirect:/admin/sales/new";
        }
    }

    @GetMapping("/sales/{id}")
    public String viewSale(@PathVariable final String id, final Model model, final HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        try {
            final UUID saleId = UUID.fromString(id);
            final FlashSaleResponseDto sale = flashSalesService.getFlashSaleById(saleId);
            model.addAttribute("sale", sale);
            model.addAttribute("products", productsService.getAllProducts());
            if (!model.containsAttribute("addFlashSaleItemDto")) {
                model.addAttribute("addFlashSaleItemDto", new AddFlashSaleItemDto("", 0, null));
            }
            return "admin/sales/detail";
        } catch (final Exception e) {
            return "redirect:/admin/sales?error=notfound";
        }
    }

    @PostMapping("/sales/{id}/items")
    public String addSaleItem(
        @PathVariable final String id,
        @Valid @ModelAttribute("addFlashSaleItemDto") final AddFlashSaleItemDto dto,
        final BindingResult bindingResult,
        final RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.addFlashSaleItemDto", bindingResult);
            redirectAttributes.addFlashAttribute("addFlashSaleItemDto", dto);
            return "redirect:/admin/sales/" + id;
        }

        try {
            final UUID saleId = UUID.fromString(id);
            flashSalesService.addItemsToFlashSale(saleId, List.of(dto));
            redirectAttributes.addFlashAttribute("success", "Flash sale item added successfully");
            return "redirect:/admin/sales/" + id;
        } catch (final IllegalArgumentException e) {
            log.warn("Cannot add item to sale {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/sales/" + id;
        } catch (final FlashSaleNotFoundException e) {
            log.warn("Flash sale not found: {}", id);
            redirectAttributes.addFlashAttribute("error", "Flash sale not found");
            return "redirect:/admin/sales/" + id;
        } catch (final ProductNotFoundException e) {
            final String msg = e.getMessage() != null && !e.getMessage().isEmpty() ? e.getMessage() : "Product not found";
            redirectAttributes.addFlashAttribute("error", msg);
            return "redirect:/admin/sales/" + id;
        } catch (final InsufficientResourcesException e) {
            redirectAttributes.addFlashAttribute("error", "Insufficient stock: " + e.getMessage());
            return "redirect:/admin/sales/" + id;
        } catch (final Exception e) {
            log.error("Error adding flash sale item", e);
            redirectAttributes.addFlashAttribute("error", "Failed to add item: " + e.getMessage());
            return "redirect:/admin/sales/" + id;
        }
    }

    @PostMapping("/sales/{id}/items/{itemId}")
    public String updateSaleItem(
        @PathVariable final String id,
        @PathVariable final String itemId,
        @RequestParam(required = false) final Integer allocatedStock,
        @RequestParam(required = false) final BigDecimal salePrice,
        final RedirectAttributes redirectAttributes) {

        if (allocatedStock == null && salePrice == null) {
            redirectAttributes.addFlashAttribute("error", "Provide at least one of allocated stock or sale price");
            return "redirect:/admin/sales/" + id;
        }

        try {
            final UUID saleUuid = UUID.fromString(id);
            final UUID itemUuid = UUID.fromString(itemId);
            final UpdateFlashSaleItemDto updateDto = new UpdateFlashSaleItemDto(allocatedStock, salePrice);
            flashSalesService.updateFlashSaleItem(saleUuid, itemUuid, updateDto);
            redirectAttributes.addFlashAttribute("success", "Flash sale item updated successfully");
            return "redirect:/admin/sales/" + id;
        } catch (final IllegalArgumentException e) {
            log.warn("Cannot update item {} in sale {}: {}", itemId, id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/sales/" + id;
        } catch (final FlashSaleNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Flash sale not found");
            return "redirect:/admin/sales/" + id;
        } catch (final FlashSaleItemNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Flash sale item not found");
            return "redirect:/admin/sales/" + id;
        } catch (final InsufficientResourcesException e) {
            redirectAttributes.addFlashAttribute("error", "Insufficient stock: " + e.getMessage());
            return "redirect:/admin/sales/" + id;
        } catch (final Exception e) {
            log.error("Error updating flash sale item", e);
            redirectAttributes.addFlashAttribute("error", "Failed to update item: " + e.getMessage());
            return "redirect:/admin/sales/" + id;
        }
    }

    @PostMapping("/sales/{id}/items/{itemId}/delete")
    public String deleteSaleItem(
        @PathVariable final String id,
        @PathVariable final String itemId,
        final RedirectAttributes redirectAttributes) {

        try {
            final UUID saleUuid = UUID.fromString(id);
            final UUID itemUuid = UUID.fromString(itemId);
            flashSalesService.removeFlashSaleItem(saleUuid, itemUuid);
            redirectAttributes.addFlashAttribute("success", "Flash sale item removed successfully");
            return "redirect:/admin/sales/" + id;
        } catch (final IllegalArgumentException e) {
            log.warn("Cannot remove item from sale {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/sales/" + id;
        } catch (final FlashSaleNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Flash sale not found");
            return "redirect:/admin/sales/" + id;
        } catch (final FlashSaleItemNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Flash sale item not found");
            return "redirect:/admin/sales/" + id;
        } catch (final Exception e) {
            log.error("Error removing flash sale item", e);
            redirectAttributes.addFlashAttribute("error", "Failed to remove item: " + e.getMessage());
            return "redirect:/admin/sales/" + id;
        }
    }

    // Orders Management
    @GetMapping("/orders")
    public String listOrders(
        @RequestParam(required = false) final String status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime endDate,
        final Model model) {

        final uk.co.aosd.flash.domain.OrderStatus orderStatus = status != null && !status.isEmpty() ? uk.co.aosd.flash.domain.OrderStatus.valueOf(status)
            : null;
        final List<OrderDetailDto> orders = orderService.getAllOrders(orderStatus, startDate, endDate, null);
        model.addAttribute("orders", orders);
        model.addAttribute("statusFilter", status);
        return "admin/orders/list";
    }

    @GetMapping("/orders/{id}")
    public String viewOrder(@PathVariable final String id, final Model model) {
        try {
            final UUID orderId = UUID.fromString(id);
            final OrderDetailDto order = orderService.getOrderByIdForAdmin(orderId);
            model.addAttribute("order", order);
            if (!model.containsAttribute("updateOrderStatusDto")) {
                model.addAttribute("updateOrderStatusDto", new UpdateOrderStatusDto(order.status()));
            }
            return "admin/orders/detail";
        } catch (final Exception e) {
            return "redirect:/admin/orders?error=notfound";
        }
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(
        @PathVariable final String id,
        @Valid @ModelAttribute final UpdateOrderStatusDto updateOrderStatusDto,
        final BindingResult bindingResult,
        final RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.updateOrderStatusDto", bindingResult);
            redirectAttributes.addFlashAttribute("updateOrderStatusDto", updateOrderStatusDto);
            return "redirect:/admin/orders/" + id;
        }

        try {
            final UUID orderId = UUID.fromString(id);
            orderService.updateOrderStatus(orderId, updateOrderStatusDto.status());
            redirectAttributes.addFlashAttribute("success", "Order status updated successfully");
            return "redirect:/admin/orders/" + id;
        } catch (final InvalidOrderStatusException e) {
            log.error("Invalid status transition: {} → {} for order {}",
                e.getCurrentStatus(), e.getRequiredStatus(), e.getOrderId());
            redirectAttributes.addFlashAttribute("error",
                String.format("Invalid status transition: %s → %s",
                    e.getCurrentStatus(), e.getRequiredStatus()));
            return "redirect:/admin/orders/" + id;
        } catch (final Exception e) {
            log.error("Error updating order status", e);
            redirectAttributes.addFlashAttribute("error", "Failed to update order status: " + e.getMessage());
            return "redirect:/admin/orders/" + id;
        }
    }

    // Analytics Dashboard
    @GetMapping("/analytics")
    public String analytics(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime endDate,
        final Model model) {

        final var defaultStartDate = startDate != null ? startDate : OffsetDateTime.now().minusMonths(1);
        final var defaultEndDate = endDate != null ? endDate : OffsetDateTime.now();

        model.addAttribute("salesMetrics", analyticsService.getSalesMetrics(defaultStartDate, defaultEndDate));
        model.addAttribute("revenueMetrics", analyticsService.getRevenueMetrics(defaultStartDate, defaultEndDate));
        model.addAttribute("orderStatistics", analyticsService.getOrderStatistics(defaultStartDate, defaultEndDate));
        model.addAttribute("productPerformance", analyticsService.getProductPerformance());
        model.addAttribute("startDate", defaultStartDate);
        model.addAttribute("endDate", defaultEndDate);
        return "admin/analytics/dashboard";
    }
}
