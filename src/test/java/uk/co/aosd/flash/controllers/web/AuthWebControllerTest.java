package uk.co.aosd.flash.controllers.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.co.aosd.flash.config.TestSecurityConfig;
import uk.co.aosd.flash.exc.DuplicateEntityException;
import uk.co.aosd.flash.services.UserService;

@WebMvcTest(controllers = AuthWebController.class)
@Import({ TestSecurityConfig.class, uk.co.aosd.flash.errorhandling.ErrorMapper.class })
@ActiveProfiles("test")
class AuthWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void loginPage_withoutError_returnsLoginViewWithLoginDto() throws Exception {
        mockMvc.perform(get("/login").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/login"))
            .andExpect(model().attributeExists("loginDto"))
            .andExpect(model().attributeDoesNotExist("error"));
    }

    @Test
    void loginPage_withError_addsErrorToModel() throws Exception {
        mockMvc.perform(get("/login").param("error", "true").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/login"))
            .andExpect(model().attribute("error", "Invalid username or password"));
    }

    @Test
    void registerPage_returnsRegisterViewWithRegisterDto() throws Exception {
        mockMvc.perform(get("/register").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/register"))
            .andExpect(model().attributeExists("registerDto"));
    }

    @Test
    void register_withValidDto_redirectsToLoginWithSuccess() throws Exception {
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("username", "johndoe")
                .param("email", "john@example.com")
                .param("password", "SecurePass123!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"))
            .andExpect(flash().attribute("success", "Registration successful! Please login."));

        verify(userService).register(any());
    }

    @Test
    void register_withBindingErrors_redirectsToRegisterWithFlashAttributes() throws Exception {
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("username", "ab")  // too short
                .param("email", "invalid-email")
                .param("password", "short"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/register"))
            .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.registerDto"))
            .andExpect(flash().attributeExists("registerDto"));

        verify(userService, never()).register(any());
    }

    @Test
    void register_whenDuplicateEntityException_redirectsWithError() throws Exception {
        when(userService.register(any())).thenThrow(new DuplicateEntityException("id", "name"));

        mockMvc.perform(post("/register")
                .with(csrf())
                .param("username", "johndoe")
                .param("email", "john@example.com")
                .param("password", "SecurePass123!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/register"))
            .andExpect(flash().attribute("error", "Username or email already exists"));
    }

    @Test
    void register_whenGenericException_redirectsWithGenericError() throws Exception {
        when(userService.register(any())).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/register")
                .with(csrf())
                .param("username", "johndoe")
                .param("email", "john@example.com")
                .param("password", "SecurePass123!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/register"))
            .andExpect(flash().attribute("error", "Registration failed. Please try again."));
    }
}
