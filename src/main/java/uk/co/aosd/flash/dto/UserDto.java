package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import uk.co.aosd.flash.domain.UserRole;

/**
 * DTO for user information.
 */
@Schema(
    name = "User",
    description = "User information."
)
public record UserDto(
    @Schema(description = "User identifier.", example = "9b2b8c2c-2f53-4a57-a07e-0a2b2b1de3a9")
    UUID id,
    
    @Schema(description = "Username.", example = "johndoe")
    String username,
    
    @Schema(description = "Email address.", example = "john.doe@example.com")
    String email,
    
    @Schema(description = "User role.")
    UserRole role,
    
    @Schema(description = "Account creation timestamp.", example = "2026-01-25T10:15:30Z")
    OffsetDateTime createdAt)
    implements Serializable {
}
