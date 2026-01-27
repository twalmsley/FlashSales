package uk.co.aosd.flash.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import uk.co.aosd.flash.repository.UserRepository;

import java.io.IOException;

/**
 * Custom authentication success handler that stores user ID in session
 * for easy access in web controllers, and persists the SecurityContext via
 * {@link SecurityContextRepository#saveContext} so that authorities (e.g. ADMIN_USER)
 * are available on the next request (e.g. after redirect to home).
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final SecurityContextRepository securityContextRepository;

    @Override
    public void onAuthenticationSuccess(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final Authentication authentication) throws IOException, ServletException {

        // Persist SecurityContext using the same repository the filter chain uses to load.
        // This ensures the redirect target (e.g. GET /) sees the authenticated user and roles.
        final var context = SecurityContextHolder.getContext();
        securityContextRepository.saveContext(context, request, response);

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
