package uk.co.aosd.flash.controllers.web;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.aosd.flash.dto.CreateOrderDto;
import uk.co.aosd.flash.security.CustomUserDetailsService;
import uk.co.aosd.flash.security.SecurityUtils;
import uk.co.aosd.flash.services.ActiveSalesService;
import uk.co.aosd.flash.services.OrderMessageSender;
import uk.co.aosd.flash.services.OrderService;
import uk.co.aosd.flash.services.ProductsService;

/**
 * Web controller for client-facing pages (sales browsing, orders).
 */
@Controller
@RequestMapping("/sales")
@RequiredArgsConstructor
public class ClientWebController {

    private static final Logger log = LoggerFactory.getLogger(ClientWebController.class);

    private final ActiveSalesService activeSalesService;
    private final ProductsService productsService;
    private final OrderService orderService;
    private final OrderMessageSender orderMessageSender;
    private final CustomUserDetailsService userDetailsService;

    @GetMapping
    public String listSales(final Model model) {
        model.addAttribute("activeSales", activeSalesService.getActiveSales());
        return "sales/list";
    }

    @GetMapping("/{itemId}")
    public String saleDetail(
        @PathVariable final String itemId,
        final Model model,
        final HttpServletRequest request) {
        final var sales = activeSalesService.getActiveSales();
        final var sale = sales.stream()
            .filter(s -> s.flashSaleItemId().equals(itemId))
            .findFirst()
            .orElse(null);

        if (sale == null) {
            return "redirect:/sales?error=notfound";
        }

        model.addAttribute("sale", sale);
        if (!model.containsAttribute("createOrderDto")) {
            model.addAttribute("createOrderDto", new CreateOrderDto(UUID.fromString(sale.flashSaleItemId()), 1));
        }

        // Use session first (same source as template's session.isAuthenticated) so we always
        // show "Your order" when the user is logged in and has an order, even if SecurityContext
        // is not populated on this request.
        final var session = request.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute("isAuthenticated"))) {
            UUID userId = (UUID) session.getAttribute("userId");
            if (userId == null) {
                try {
                    userId = getCurrentUserId();
                } catch (final Exception e) {
                    log.debug("Could not resolve current user for existing order check: {}", e.getMessage());
                }
            }
            if (userId != null) {
                try {
                    orderService.findOrderByUserAndFlashSaleItem(userId, UUID.fromString(itemId))
                        .ifPresent(order -> model.addAttribute("existingOrder", order));
                } catch (final Exception e) {
                    log.warn("Failed to load existing order for sale detail: {}", e.getMessage());
                }
            }
        }

        return "sales/detail";
    }

    @PostMapping("/{itemId}/order")
    public String createOrder(
        @PathVariable final String itemId,
        @Valid @ModelAttribute final CreateOrderDto createOrderDto,
        final BindingResult bindingResult,
        final RedirectAttributes redirectAttributes,
        final Authentication authentication) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.createOrderDto", bindingResult);
            redirectAttributes.addFlashAttribute("createOrderDto", createOrderDto);
            return "redirect:/sales/" + itemId;
        }

        try {
            final UUID userId = getCurrentUserId();
            final var order = orderService.createOrder(createOrderDto, userId);
            orderMessageSender.sendForProcessing(order.orderId());
            redirectAttributes.addFlashAttribute("success", "Order created successfully! Order ID: " + order.orderId());
            return "redirect:/orders";
        } catch (final Exception e) {
            log.error("Error creating order", e);
            redirectAttributes.addFlashAttribute("error", "Failed to create order: " + e.getMessage());
            redirectAttributes.addFlashAttribute("createOrderDto", createOrderDto);
            return "redirect:/sales/" + itemId;
        }
    }

    private UUID getCurrentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (final IllegalStateException e) {
            // Form login - look up user ID from username
            final String username = SecurityUtils.getCurrentUsername();
            final UUID userId = userDetailsService.getUserIdByUsername(username);
            if (userId == null) {
                throw new IllegalStateException("User not found: " + username);
            }
            return userId;
        }
    }
}
