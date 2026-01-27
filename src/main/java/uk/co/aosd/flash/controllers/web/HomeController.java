package uk.co.aosd.flash.controllers.web;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
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
    public String home(final Model model, final HttpServletResponse response, final Authentication authentication) {
        // Prevent caching so role-based content (e.g. admin links) is never served from
        // cache.
        response.setHeader("Cache-Control", "private, no-store");
        model.addAttribute("activeSales", activeSalesService.getActiveSales());
        final boolean isAuthenticated = authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
        model.addAttribute("isAuthenticated", isAuthenticated);
        boolean showAdminLinks = false;
        if (isAuthenticated && authentication != null) {
            showAdminLinks = AuthorityUtils.authorityListToSet(authentication.getAuthorities())
                .contains("ROLE_ADMIN_USER");
        }
        model.addAttribute("showAdminLinks", showAdminLinks);
        return "home";
    }
}
