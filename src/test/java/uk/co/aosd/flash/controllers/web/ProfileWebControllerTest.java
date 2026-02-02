package uk.co.aosd.flash.controllers.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.co.aosd.flash.config.TestSecurityConfig;
import uk.co.aosd.flash.domain.UserRole;
import uk.co.aosd.flash.dto.UserDto;
import uk.co.aosd.flash.exc.DuplicateEntityException;
import uk.co.aosd.flash.exc.InvalidCurrentPasswordException;
import uk.co.aosd.flash.security.CustomUserDetailsService;
import uk.co.aosd.flash.services.UserService;

@WebMvcTest(controllers = ProfileWebController.class)
@Import({ TestSecurityConfig.class, uk.co.aosd.flash.errorhandling.ErrorMapper.class })
@ActiveProfiles("test")
class ProfileWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UserDto USER_DTO = new UserDto(
        USER_ID, "johndoe", "john@example.com", UserRole.USER, OffsetDateTime.now());

    @Test
    void viewProfile_whenAuthenticated_returnsProfileView() throws Exception {
        when(userDetailsService.getUserIdByUsername("johndoe")).thenReturn(USER_ID);
        when(userService.findById(USER_ID)).thenReturn(USER_DTO);

        mockMvc.perform(get("/profile").with(csrf()).with(user("johndoe").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(view().name("profile/view"))
            .andExpect(model().attribute("profile", USER_DTO));
    }

    @Test
    void editProfileForm_whenAuthenticated_returnsEditViewWithDto() throws Exception {
        when(userDetailsService.getUserIdByUsername("johndoe")).thenReturn(USER_ID);
        when(userService.findById(USER_ID)).thenReturn(USER_DTO);

        mockMvc.perform(get("/profile/edit").with(csrf()).with(user("johndoe").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(view().name("profile/edit"))
            .andExpect(model().attributeExists("updateProfileDto"))
            .andExpect(model().attribute("profile", USER_DTO));
    }

    @Test
    void updateProfile_withValidData_redirectsToProfileWithSuccess() throws Exception {
        when(userDetailsService.getUserIdByUsername("johndoe")).thenReturn(USER_ID);
        when(userService.updateProfile(eq(USER_ID), any())).thenReturn(
            new UserDto(USER_ID, "newuser", "new@example.com", UserRole.USER, USER_DTO.createdAt()));

        mockMvc.perform(post("/profile/edit")
                .with(csrf())
                .with(user("johndoe").roles("USER"))
                .param("username", "newuser")
                .param("email", "new@example.com")
                .param("currentPassword", "CurrentPass123!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/profile"))
            .andExpect(flash().attribute("success", "Profile updated successfully."));

        verify(userService).updateProfile(eq(USER_ID), any());
    }

    @Test
    void updateProfile_wrongCurrentPassword_redirectsToEditWithError() throws Exception {
        when(userDetailsService.getUserIdByUsername("johndoe")).thenReturn(USER_ID);
        when(userService.updateProfile(eq(USER_ID), any())).thenThrow(new InvalidCurrentPasswordException());

        mockMvc.perform(post("/profile/edit")
                .with(csrf())
                .with(user("johndoe").roles("USER"))
                .param("username", "newuser")
                .param("email", "new@example.com")
                .param("currentPassword", "WrongPass123!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/profile/edit"))
            .andExpect(flash().attribute("error", "Current password is incorrect."));
    }

    @Test
    void updateProfile_duplicateUsername_redirectsToEditWithError() throws Exception {
        when(userDetailsService.getUserIdByUsername("johndoe")).thenReturn(USER_ID);
        when(userService.updateProfile(eq(USER_ID), any())).thenThrow(new DuplicateEntityException("username", "taken"));

        mockMvc.perform(post("/profile/edit")
                .with(csrf())
                .with(user("johndoe").roles("USER"))
                .param("username", "taken")
                .param("email", "john@example.com")
                .param("currentPassword", "CurrentPass123!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/profile/edit"))
            .andExpect(flash().attribute("error", "Username already taken."));
    }

    @Test
    void changePasswordForm_whenAuthenticated_returnsPasswordView() throws Exception {
        mockMvc.perform(get("/profile/password").with(csrf()).with(user("johndoe").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(view().name("profile/password"))
            .andExpect(model().attributeExists("changePasswordDto"));
    }

    @Test
    void changePassword_withValidData_redirectsToProfileWithSuccess() throws Exception {
        when(userDetailsService.getUserIdByUsername("johndoe")).thenReturn(USER_ID);

        mockMvc.perform(post("/profile/password")
                .with(csrf())
                .with(user("johndoe").roles("USER"))
                .param("currentPassword", "CurrentPass123!")
                .param("newPassword", "NewSecurePass456!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/profile"))
            .andExpect(flash().attribute("success", "Password changed successfully."));

        verify(userService).changePassword(eq(USER_ID), any());
    }

    @Test
    void changePassword_wrongCurrentPassword_redirectsToPasswordWithError() throws Exception {
        when(userDetailsService.getUserIdByUsername("johndoe")).thenReturn(USER_ID);
        doThrow(new InvalidCurrentPasswordException()).when(userService).changePassword(eq(USER_ID), any());

        mockMvc.perform(post("/profile/password")
                .with(csrf())
                .with(user("johndoe").roles("USER"))
                .param("currentPassword", "WrongPass123!")
                .param("newPassword", "NewSecurePass456!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/profile/password"))
            .andExpect(flash().attribute("error", "Current password is incorrect."));
    }
}
