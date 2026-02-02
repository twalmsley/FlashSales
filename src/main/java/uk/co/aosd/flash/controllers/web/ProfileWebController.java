package uk.co.aosd.flash.controllers.web;

import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.aosd.flash.dto.ChangePasswordDto;
import uk.co.aosd.flash.dto.UpdateProfileDto;
import uk.co.aosd.flash.dto.UserDto;
import uk.co.aosd.flash.exc.DuplicateEntityException;
import uk.co.aosd.flash.exc.InvalidCurrentPasswordException;
import uk.co.aosd.flash.security.CustomUserDetailsService;
import uk.co.aosd.flash.security.SecurityUtils;
import uk.co.aosd.flash.services.UserService;

/**
 * Web controller for profile view, edit, and password change.
 */
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j(topic = "ProfileWebController")
public class ProfileWebController {

    private final UserService userService;
    private final CustomUserDetailsService userDetailsService;

    private UUID getCurrentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (final IllegalStateException e) {
            log.error("Error getting current user ID", e);
            final String username = SecurityUtils.getCurrentUsername();
            final UUID userId = userDetailsService.getUserIdByUsername(username);
            if (userId == null) {
                throw new IllegalStateException("User not found: " + username);
            }
            return userId;
        }
    }

    @GetMapping
    public String viewProfile(final Model model) {
        final UUID userId = getCurrentUserId();
        final UserDto profile = userService.findById(userId);
        model.addAttribute("profile", profile);
        return "profile/view";
    }

    @GetMapping("/edit")
    public String editProfileForm(final Model model) {
        final UUID userId = getCurrentUserId();
        final UserDto profile = userService.findById(userId);
        if (!model.containsAttribute("updateProfileDto")) {
            model.addAttribute("updateProfileDto",
                new UpdateProfileDto(profile.username(), profile.email(), ""));
        }
        model.addAttribute("profile", profile);
        return "profile/edit";
    }

    @PostMapping("/edit")
    public String updateProfile(
        @Valid @ModelAttribute final UpdateProfileDto updateProfileDto,
        final BindingResult bindingResult,
        final RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.updateProfileDto",
                bindingResult);
            redirectAttributes.addFlashAttribute("updateProfileDto", updateProfileDto);
            redirectAttributes.addFlashAttribute("error", "Please fix the errors below.");
            return "redirect:/profile/edit";
        }

        try {
            final UUID userId = getCurrentUserId();
            userService.updateProfile(userId, updateProfileDto);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
            return "redirect:/profile";
        } catch (final InvalidCurrentPasswordException e) {
            log.warn("Wrong current password on profile update");
            redirectAttributes.addFlashAttribute("error", "Current password is incorrect.");
            redirectAttributes.addFlashAttribute("updateProfileDto", updateProfileDto);
            return "redirect:/profile/edit";
        } catch (final DuplicateEntityException e) {
            final String msg = "username".equals(e.getId()) ? "Username already taken." : "Email already in use.";
            redirectAttributes.addFlashAttribute("error", msg);
            redirectAttributes.addFlashAttribute("updateProfileDto", updateProfileDto);
            return "redirect:/profile/edit";
        }
    }

    @GetMapping("/password")
    public String changePasswordForm(final Model model) {
        if (!model.containsAttribute("changePasswordDto")) {
            model.addAttribute("changePasswordDto", new ChangePasswordDto("", ""));
        }
        return "profile/password";
    }

    @PostMapping("/password")
    public String changePassword(
        @Valid @ModelAttribute final ChangePasswordDto changePasswordDto,
        final BindingResult bindingResult,
        final RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.changePasswordDto",
                bindingResult);
            redirectAttributes.addFlashAttribute("changePasswordDto", changePasswordDto);
            redirectAttributes.addFlashAttribute("error", "Please fix the errors below.");
            return "redirect:/profile/password";
        }

        try {
            final UUID userId = getCurrentUserId();
            userService.changePassword(userId, changePasswordDto);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
            return "redirect:/profile";
        } catch (final InvalidCurrentPasswordException e) {
            log.warn("Wrong current password on password change");
            redirectAttributes.addFlashAttribute("error", "Current password is incorrect.");
            redirectAttributes.addFlashAttribute("changePasswordDto", changePasswordDto);
            return "redirect:/profile/password";
        }
    }
}
