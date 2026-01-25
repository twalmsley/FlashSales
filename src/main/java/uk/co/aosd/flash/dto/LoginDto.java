package uk.co.aosd.flash.dto;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for user login.
 */
@Schema(
    name = "Login",
    description = "Request body for user login."
)
public record LoginDto(
    @Schema(description = "Username or email.", example = "johndoe")
    @NotBlank(message = "Username or email is required")
    String username,
    
    @Schema(description = "Password.", example = "SecurePassword123!")
    @NotBlank(message = "Password is required")
    String password)
    implements Serializable {
}
