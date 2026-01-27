package uk.co.aosd.flash.controllers.web;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import uk.co.aosd.flash.services.ActiveSalesService;

/**
 * Web controller for public home page.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ActiveSalesService activeSalesService;

    @GetMapping("/")
    public String home(final Model model, final HttpServletResponse response) {
        // Prevent caching so role-based content (e.g. admin links) is never served from cache.
        response.setHeader("Cache-Control", "private, no-store");
        model.addAttribute("activeSales", activeSalesService.getActiveSales());
        return "home";
    }
}
