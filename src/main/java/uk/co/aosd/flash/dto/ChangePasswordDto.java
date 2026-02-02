package uk.co.aosd.flash.dto;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for changing user password.
 */
@Schema(
    name = "ChangePassword",
    description = "Request body for changing password. Current password is required for verification."
)
public record ChangePasswordDto(
    @Schema(description = "Current password (required for verification).", example = "CurrentPassword123!")
    @NotBlank(message = "Current password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    String currentPassword,

    @Schema(description = "New password.", example = "NewSecurePassword456!")
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
    String newPassword)
    implements Serializable {
}
