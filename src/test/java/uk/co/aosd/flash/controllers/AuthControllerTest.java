package uk.co.aosd.flash.controllers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import uk.co.aosd.flash.config.TestSecurityConfig;
import uk.co.aosd.flash.domain.User;
import uk.co.aosd.flash.domain.UserRole;
import uk.co.aosd.flash.dto.AuthResponseDto;
import uk.co.aosd.flash.dto.LoginDto;
import uk.co.aosd.flash.dto.RegisterDto;
import uk.co.aosd.flash.repository.UserRepository;
import uk.co.aosd.flash.util.TestJwtUtils;

/**
 * Integration tests for authentication endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@Import(TestSecurityConfig.class)
@ActiveProfiles({ "test", "admin-service", "api-service" })
@Transactional
public class AuthControllerTest {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres");

    @Container
    @ServiceConnection(name = "redis")
    @SuppressWarnings("resource")
    public static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    private static ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "TestPassword123!";

    @BeforeAll
    public static void beforeAll() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        TestJwtUtils.clearSecurityContext();
    }

    @Test
    void shouldRegisterNewUser() throws Exception {
        final RegisterDto registerDto = new RegisterDto(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);

        final String response = mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.userId").exists())
            .andExpect(jsonPath("$.username").value(TEST_USERNAME))
            .andExpect(jsonPath("$.email").value(TEST_EMAIL))
            .andExpect(jsonPath("$.role").value("USER"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        final AuthResponseDto authResponse = objectMapper.readValue(response, AuthResponseDto.class);
        assertNotNull(authResponse.token());
        assertNotNull(authResponse.userId());
    }

    @Test
    void shouldFailRegistrationWithDuplicateUsername() throws Exception {
        // Create first user
        final User existingUser = new User();
        existingUser.setUsername(TEST_USERNAME);
        existingUser.setEmail("other@example.com");
        existingUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        existingUser.setRoles(UserRole.USER);
        userRepository.save(existingUser);

        final RegisterDto registerDto = new RegisterDto(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerDto)))
            .andExpect(status().isConflict());
    }

    @Test
    void shouldFailRegistrationWithDuplicateEmail() throws Exception {
        // Create first user
        final User existingUser = new User();
        existingUser.setUsername("otheruser");
        existingUser.setEmail(TEST_EMAIL);
        existingUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        existingUser.setRoles(UserRole.USER);
        userRepository.save(existingUser);

        final RegisterDto registerDto = new RegisterDto(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerDto)))
            .andExpect(status().isConflict());
    }

    @Test
    void shouldLoginWithUsername() throws Exception {
        // Create user
        final User user = new User();
        user.setUsername(TEST_USERNAME);
        user.setEmail(TEST_EMAIL);
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setRoles(UserRole.USER);
        userRepository.save(user);

        final LoginDto loginDto = new LoginDto(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.userId").exists())
            .andExpect(jsonPath("$.username").value(TEST_USERNAME))
            .andExpect(jsonPath("$.email").value(TEST_EMAIL))
            .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void shouldLoginWithEmail() throws Exception {
        // Create user
        final User user = new User();
        user.setUsername(TEST_USERNAME);
        user.setEmail(TEST_EMAIL);
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setRoles(UserRole.USER);
        userRepository.save(user);

        final LoginDto loginDto = new LoginDto(TEST_EMAIL, TEST_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.username").value(TEST_USERNAME));
    }

    @Test
    void shouldFailLoginWithInvalidCredentials() throws Exception {
        final LoginDto loginDto = new LoginDto(TEST_USERNAME, "WrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldGetCurrentUser() throws Exception {
        // Create user
        final User user = new User();
        user.setUsername(TEST_USERNAME);
        user.setEmail(TEST_EMAIL);
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setRoles(UserRole.USER);
        final User savedUser = userRepository.save(user);

        // Set up security context
        TestJwtUtils.setSecurityContext(savedUser.getId(), UserRole.USER);

        mockMvc.perform(get("/api/v1/auth/me")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(savedUser.getId().toString()))
            .andExpect(jsonPath("$.username").value(TEST_USERNAME))
            .andExpect(jsonPath("$.email").value(TEST_EMAIL))
            .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void shouldFailGetCurrentUserWhenUnauthenticated() throws Exception {
        TestJwtUtils.clearSecurityContext();

        mockMvc.perform(get("/api/v1/auth/me")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }
}
