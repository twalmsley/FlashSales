package uk.co.aosd.flash.dto;

import java.io.Serializable;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import uk.co.aosd.flash.domain.UserRole;

/**
 * DTO for authentication response (login/register).
 */
@Schema(
    name = "AuthResponse",
    description = "Response containing JWT token and user information."
)
public record AuthResponseDto(
    @Schema(description = "JWT access token.", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String token,
    
    @Schema(description = "User identifier.", example = "9b2b8c2c-2f53-4a57-a07e-0a2b2b1de3a9")
    UUID userId,
    
    @Schema(description = "Username.", example = "johndoe")
    String username,
    
    @Schema(description = "Email address.", example = "john.doe@example.com")
    String email,
    
    @Schema(description = "User role.")
    UserRole role)
    implements Serializable {
}
