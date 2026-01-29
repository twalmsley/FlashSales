package uk.co.aosd.flash.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration test for CORS configuration on API endpoints.
 * Uses real SecurityConfig and CorsConfig (profiles admin-service, api-service)
 * to assert that cross-origin requests receive correct CORS headers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles({"admin-service", "api-service"})
class CorsConfigTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";
    private static final String DISALLOWED_ORIGIN = "http://disallowed.example.com";

    @Container
    @ServiceConnection
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:latest");

    @Container
    @ServiceConnection(name = "redis")
    @SuppressWarnings("resource")
    public static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:latest"))
        .withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnCorsHeadersForAllowedOriginOnApiRequest() throws Exception {
        // Unauthenticated request to /api/v1/auth/me returns 401 or 403; we assert CORS header is present
        mockMvc.perform(get("/api/v1/auth/me").header("Origin", ALLOWED_ORIGIN))
            .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    @Test
    void shouldReturnCorsHeadersOnPreflightRequestWithoutAuthentication() throws Exception {
        ResultActions result = mockMvc.perform(
            options("/api/v1/auth/login")
                .header("Origin", ALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"));
        result
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
            .andExpect(header().exists("Access-Control-Allow-Methods"))
            .andExpect(header().exists("Access-Control-Allow-Headers"))
            .andExpect(header().exists("Access-Control-Max-Age"));
    }

    @Test
    void shouldNotExposeAllowOriginForDisallowedOrigin() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/auth/me").header("Origin", DISALLOWED_ORIGIN));
        String allowOrigin = result.andReturn().getResponse().getHeader("Access-Control-Allow-Origin");
        // Disallowed origin must not get Access-Control-Allow-Origin (or must not be the disallowed origin)
        assert allowOrigin == null || !DISALLOWED_ORIGIN.equals(allowOrigin)
            : "Disallowed origin must not receive Access-Control-Allow-Origin";
    }
}
