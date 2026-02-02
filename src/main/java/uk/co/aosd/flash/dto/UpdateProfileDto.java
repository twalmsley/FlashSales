package uk.co.aosd.flash.dto;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for updating user profile (username and/or email).
 */
@Schema(
    name = "UpdateProfile",
    description = "Request body for updating profile (username, email). Current password is required."
)
public record UpdateProfileDto(
    @Schema(description = "New username (optional; leave blank to keep current).", example = "johndoe")
    @Size(max = 50, message = "Username must not exceed 50 characters")
    String username,

    @Schema(description = "New email address (optional; leave blank to keep current).", example = "john.doe@example.com")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    String email,

    @Schema(description = "Current password (required for verification).", example = "CurrentPassword123!")
    @NotBlank(message = "Current password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    String currentPassword)
    implements Serializable {
}
