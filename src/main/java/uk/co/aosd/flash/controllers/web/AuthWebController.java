package uk.co.aosd.flash.controllers.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.aosd.flash.dto.LoginDto;
import uk.co.aosd.flash.dto.RegisterDto;
import uk.co.aosd.flash.exc.DuplicateEntityException;
import uk.co.aosd.flash.services.UserService;

/**
 * Web controller for authentication pages (login, register).
 */
@Controller
@RequiredArgsConstructor
public class AuthWebController {

    private static final Logger log = LoggerFactory.getLogger(AuthWebController.class);

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(final Model model, final String error) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (!model.containsAttribute("loginDto")) {
            model.addAttribute("loginDto", new LoginDto("", ""));
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(final Model model) {
        if (!model.containsAttribute("registerDto")) {
            model.addAttribute("registerDto", new RegisterDto("", "", ""));
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
        @Valid @ModelAttribute final RegisterDto registerDto,
        final BindingResult bindingResult,
        final RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registerDto", bindingResult);
            redirectAttributes.addFlashAttribute("registerDto", registerDto);
            return "redirect:/register";
        }

        try {
            userService.register(registerDto);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (final DuplicateEntityException e) {
            log.warn("Registration failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Username or email already exists");
            redirectAttributes.addFlashAttribute("registerDto", registerDto);
            return "redirect:/register";
        } catch (final Exception e) {
            log.error("Registration error", e);
            redirectAttributes.addFlashAttribute("error", "Registration failed. Please try again.");
            redirectAttributes.addFlashAttribute("registerDto", registerDto);
            return "redirect:/register";
        }
    }
}
