package uk.co.aosd.flash.controllers.web;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.aosd.flash.security.CustomUserDetailsService;
import uk.co.aosd.flash.security.SecurityUtils;
import uk.co.aosd.flash.services.OrderService;

/**
 * Web controller for order pages.
 */
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j(topic = "OrdersWebController")
public class OrdersWebController {

    private final OrderService orderService;
    private final CustomUserDetailsService userDetailsService;

    private UUID getCurrentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (final IllegalStateException e) {
            log.error("Error getting current user ID", e);
            // Form login - look up user ID from username
            final String username = SecurityUtils.getCurrentUsername();
            final UUID userId = userDetailsService.getUserIdByUsername(username);
            if (userId == null) {
                throw new IllegalStateException("User not found: " + username);
            }
            return userId;
        }
    }

    @GetMapping
    public String listOrders(final Model model) {
        final UUID userId = getCurrentUserId();
        model.addAttribute("orders", orderService.getOrdersByUser(userId, null, null, null));
        return "orders/list";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable final String id, final Model model) {
        try {
            final UUID userId = getCurrentUserId();
            final UUID orderId = UUID.fromString(id);
            model.addAttribute("order", orderService.getOrderById(orderId, userId));
            return "orders/detail";
        } catch (final Exception e) {
            return "redirect:/orders?error=notfound";
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable final String id, final RedirectAttributes redirectAttributes) {
        try {
            final UUID userId = getCurrentUserId();
            final UUID orderId = UUID.fromString(id);
            // Validate ownership
            orderService.getOrderById(orderId, userId);
            orderService.handleCancel(orderId);
            redirectAttributes.addFlashAttribute("success", "Order cancelled successfully");
            return "redirect:/orders";
        } catch (final IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid order");
            return "redirect:/orders";
        } catch (final Exception e) {
            log.warn("Cancel failed for order {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage() != null ? e.getMessage() : "Failed to cancel order");
            return "redirect:/orders/" + id;
        }
    }
}
