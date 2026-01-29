package uk.co.aosd.flash.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationEntryPointTest {

    private JwtAuthenticationEntryPoint entryPoint;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        entryPoint = new JwtAuthenticationEntryPoint();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void commence_setsStatus401AndJsonContentType() throws IOException, ServletException {
        final AuthenticationException authException = new BadCredentialsException("Bad credentials");

        entryPoint.commence(request, response, authException);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void commence_writesErrorBodyWithExpectedFields() throws IOException, ServletException {
        final AuthenticationException authException = new BadCredentialsException("Bad credentials");

        entryPoint.commence(request, response, authException);

        final String body = response.getContentAsString();
        assertThat(body).contains("Unauthorized");
        assertThat(body).contains("Authentication required");
        assertThat(body).contains("error");
        assertThat(body).contains("message");
    }
}
