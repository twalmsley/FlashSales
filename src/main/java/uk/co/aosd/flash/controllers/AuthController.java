package uk.co.aosd.flash.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import uk.co.aosd.flash.dto.AuthResponseDto;
import uk.co.aosd.flash.dto.LoginDto;
import uk.co.aosd.flash.dto.RegisterDto;
import uk.co.aosd.flash.dto.UserDto;
import uk.co.aosd.flash.security.SecurityUtils;
import uk.co.aosd.flash.services.UserService;

/**
 * Authentication REST API.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(
    name = "Authentication",
    description = "User registration, login, and authentication endpoints."
)
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    /**
     * Register a new user.
     *
     * @param registerDto registration data
     * @return authentication response with JWT token
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register new user",
        description = "Creates a new user account and returns a JWT token."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "User registered successfully.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Username or email already exists.",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Validation error.",
            content = @Content
        )
    })
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody final RegisterDto registerDto) {
        log.info("Registration request for username: {}", registerDto.username());
        try {
            final AuthResponseDto response = userService.register(registerDto);
            log.info("User registered successfully: {}", registerDto.username());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (final Exception e) {
            log.error("Registration failed for username: {}", registerDto.username(), e);
            throw e;
        }
    }

    /**
     * Login with username/email and password.
     *
     * @param loginDto login credentials
     * @return authentication response with JWT token
     */
    @PostMapping("/login")
    @Operation(
        summary = "Login",
        description = "Authenticates a user and returns a JWT token."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials.",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Validation error.",
            content = @Content
        )
    })
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody final LoginDto loginDto) {
        log.info("Login request for username: {}", loginDto.username());
        try {
            final AuthResponseDto response = userService.authenticate(loginDto);
            log.info("Login successful for username: {}", loginDto.username());
            return ResponseEntity.ok(response);
        } catch (final BadCredentialsException e) {
            log.warn("Login failed for username: {}", loginDto.username());
            throw e;
        } catch (final Exception e) {
            log.error("Login error for username: {}", loginDto.username(), e);
            throw e;
        }
    }

    /**
     * Get current authenticated user information.
     *
     * @return current user information
     */
    @GetMapping("/me")
    @Operation(
        summary = "Get current user",
        description = "Returns information about the currently authenticated user."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Current user information.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized.",
            content = @Content
        )
    })
    public ResponseEntity<UserDto> getCurrentUser() {
        final var userId = SecurityUtils.getCurrentUserId();
        log.info("Getting current user info for ID: {}", userId);
        final UserDto userDto = userService.findById(userId);
        return ResponseEntity.ok(userDto);
    }
}
