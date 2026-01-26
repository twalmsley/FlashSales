package uk.co.aosd.flash.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import uk.co.aosd.flash.repository.UserRepository;

import java.io.IOException;
import java.util.UUID;

/**
 * Custom authentication success handler that stores user ID in session
 * for easy access in web controllers.
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final Authentication authentication) throws IOException, ServletException {
        
        // Store user ID in session for easy access
        if (authentication.getPrincipal() instanceof UserDetails) {
            final String username = ((UserDetails) authentication.getPrincipal()).getUsername();
            userRepository.findByUsername(username)
                .ifPresent(user -> request.getSession().setAttribute("userId", user.getId()));
        }
        
        // Redirect to home page
        response.sendRedirect("/");
    }
}
