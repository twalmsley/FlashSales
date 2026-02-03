package uk.co.aosd.flash.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration test for actuator endpoint security.
 * Tests that actuator endpoints are properly secured and require ADMIN_USER role.
 * 
 * Note: This test uses a profile that includes SecurityConfig (not "test" profile)
 * to test the actual security configuration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles({"admin-service", "api-service"}) // Exclude "test" profile to use real SecurityConfig
public class ActuatorSecurityTest {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:latest");

    @Container
    @ServiceConnection(name = "redis")
    @SuppressWarnings("resource")
    public static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:latest"))
        .withExposedPorts(6379);

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    public static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq:latest"));

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldAllowPublicAccessToHealthEndpoint() throws Exception {
        // Health endpoint should be publicly accessible for Kubernetes probes
        // May return 200 (healthy) or 503 (unhealthy) depending on service status
        // Important: Should NOT return 302 (redirect to login) or 403 (forbidden)
        final int status = mockMvc.perform(get("/actuator/health"))
            .andReturn()
            .getResponse()
            .getStatus();
        assert status == 200 || status == 503 : "Health endpoint should be accessible (200 or 503), got " + status;
    }

    @Test
    public void shouldAllowPublicAccessToHealthLivenessEndpoint() throws Exception {
        // Health liveness endpoint should be publicly accessible
        final int status = mockMvc.perform(get("/actuator/health/liveness"))
            .andReturn()
            .getResponse()
            .getStatus();
        assert status == 200 || status == 503 : "Health liveness should be accessible (200 or 503), got " + status;
    }

    @Test
    public void shouldAllowPublicAccessToHealthReadinessEndpoint() throws Exception {
        // Health readiness endpoint should be publicly accessible
        final int status = mockMvc.perform(get("/actuator/health/readiness"))
            .andReturn()
            .getResponse()
            .getStatus();
        assert status == 200 || status == 503 : "Health readiness should be accessible (200 or 503), got " + status;
    }

    @Test
    public void shouldReportReadinessUpWhenExternalServicesAreUp() throws Exception {
        // Readiness group includes ping, db, redis, rabbit; with all containers up, readiness is UP (200)
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    public void shouldDenyUnauthenticatedAccessToMetricsEndpoint() throws Exception {
        // Metrics endpoint should require authentication
        mockMvc.perform(get("/actuator/metrics"))
            .andExpect(status().isFound()); // Redirects to login page (302)
    }

    @Test
    public void shouldDenyUnauthenticatedAccessToInfoEndpoint() throws Exception {
        // Info endpoint should require authentication
        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isFound()); // Redirects to login page (302)
    }

    @Test
    public void shouldDenyNonAdminUserAccessToMetricsEndpoint() throws Exception {
        // Regular USER role should not have access to actuator endpoints
        mockMvc.perform(get("/actuator/metrics")
            .with(user("user").roles("USER")))
            .andExpect(status().isForbidden()); // 403 Forbidden
    }

    @Test
    public void shouldAllowAdminUserAccessToMetricsEndpoint() throws Exception {
        // ADMIN_USER role should have access to actuator endpoints
        // Endpoint may return 200 (if enabled) or 404 (if not enabled)
        // Important: Should NOT return 302 (redirect) or 403 (forbidden)
        final int status = mockMvc.perform(get("/actuator/metrics")
            .with(user("admin").roles("ADMIN_USER")))
            .andReturn()
            .getResponse()
            .getStatus();
        assert (status == 200 || status == 404) && status != 302 && status != 403
            : "Admin should access metrics (200 or 404), not be redirected (302) or forbidden (403), got " + status;
    }

    @Test
    public void shouldAllowAdminUserAccessToInfoEndpoint() throws Exception {
        // ADMIN_USER role should have access to actuator endpoints
        // Endpoint may return 200 (if enabled) or 404 (if not enabled)
        // Important: Should NOT return 302 (redirect) or 403 (forbidden)
        final int status = mockMvc.perform(get("/actuator/info")
            .with(user("admin").roles("ADMIN_USER")))
            .andReturn()
            .getResponse()
            .getStatus();
        assert (status == 200 || status == 404) && status != 302 && status != 403
            : "Admin should access info (200 or 404), not be redirected (302) or forbidden (403), got " + status;
    }

    @Test
    public void shouldDenyNonAdminUserAccessToInfoEndpoint() throws Exception {
        // Regular USER role should not have access to actuator endpoints
        mockMvc.perform(get("/actuator/info")
            .with(user("user").roles("USER")))
            .andExpect(status().isForbidden()); // 403 Forbidden
    }

    @Test
    public void shouldDenyUnauthenticatedAccessToPrometheusEndpoint() throws Exception {
        // Prometheus endpoint should require authentication (same as other actuator endpoints)
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isFound()); // Redirects to login page (302)
    }

    @Test
    public void shouldDenyNonAdminUserAccessToPrometheusEndpoint() throws Exception {
        // Regular USER role should not have access to Prometheus scrape endpoint
        mockMvc.perform(get("/actuator/prometheus")
            .with(user("user").roles("USER")))
            .andExpect(status().isForbidden()); // 403 Forbidden
    }

    @Test
    public void shouldAllowAdminUserAccessToPrometheusEndpoint() throws Exception {
        // ADMIN_USER role should have access to Prometheus endpoint when it is exposed on this server.
        // When management.server.port is set (e.g. 8081), actuator is on a different port so we get 404 here.
        final int status = mockMvc.perform(get("/actuator/prometheus")
            .with(user("admin").roles("ADMIN_USER")))
            .andReturn()
            .getResponse()
            .getStatus();
        assert status == 200 || status == 404 : "Admin should access prometheus (200) or 404 if management is on another port, got " + status;
    }
}
