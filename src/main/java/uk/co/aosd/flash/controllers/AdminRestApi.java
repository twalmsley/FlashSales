package uk.co.aosd.flash.controllers;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * An Admin REST API.
 */
@RestController
@Profile("admin-service")
@RequestMapping("/api/v1/admin")
@Tag(
    name = "Admin - Status",
    description = "Admin service status endpoint(s)."
)
public class AdminRestApi {

    public AdminRestApi() {
    }

    @GetMapping("admin_api_status")
    @Operation(
        summary = "Admin API status",
        description = "Simple readiness check for the admin service."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Service is ready.",
        content = @Content(
            mediaType = "text/plain",
            schema = @Schema(implementation = String.class),
            examples = @ExampleObject(value = "Ready")
        )
    )
    public ResponseEntity<String> adminApiStatus() {
        return ResponseEntity.ok("Ready");
    }
}
