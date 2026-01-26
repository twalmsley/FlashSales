package uk.co.aosd.flash.controllers.web;

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
    public String home(final Model model) {
        model.addAttribute("activeSales", activeSalesService.getActiveSales());
        return "home";
    }
}
