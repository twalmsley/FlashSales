package uk.co.aosd.flash.controllers;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.dto.ErrorResponseDto;
import uk.co.aosd.flash.exc.DuplicateEntityException;
import uk.co.aosd.flash.exc.ProductNotFoundException;
import uk.co.aosd.flash.services.ProductsService;

/**
 * A REST API for products.
 */
@RestController
@Profile("admin-service")
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(
    name = "Products (Admin)",
    description = "Admin endpoints for managing products."
)
public class ProductRestApi {

    private static Logger log = LoggerFactory.getLogger(ProductRestApi.class.getName());

    private final ProductsService service;

    /**
     * API for creating a product.
     */
    @PostMapping
    @Operation(
        summary = "Create product",
        description = "Creates a new product and returns a Location header pointing to the created resource."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Product created.",
            headers = @Header(
                name = "Location",
                description = "URI of the created product resource.",
                schema = @Schema(type = "string")
            ),
            content = @Content
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Duplicate product (same id/name).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Validation error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Malformed request or invalid input.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
    })
    public ResponseEntity<String> createProduct(@Valid @RequestBody final ProductDto product) throws DuplicateEntityException {
        // Logic to save product
        final var uuid = service.createProduct(product);
        log.info("Created Product: " + uuid);
        return ResponseEntity.created(URI.create("/api/v1/products/" + uuid.toString())).build();
    }

    /**
     * API for getting all products.
     */
    @GetMapping
    @Operation(summary = "List products", description = "Returns all products.")
    @ApiResponse(
        responseCode = "200",
        description = "List of products.",
        content = @Content(
            mediaType = "application/json",
            array = @ArraySchema(schema = @Schema(implementation = ProductDto.class))
        )
    )
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        log.info("Returned a list of all products.");
        // Logic to return all products
        return ResponseEntity.ok(service.getAllProducts());
    }

    /**
     * API for getting a specific product by id.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get product by id", description = "Returns a single product by id.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found.",
            content = @Content
        )
    })
    public ResponseEntity<Optional<ProductDto>> getProductById(
        @Parameter(description = "Product identifier.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @PathVariable final String id) {
        // Logic to return a single product by ID
        final Optional<ProductDto> productById = service.getProductById(id);
        if (productById.isEmpty()) {
            log.info("Failed to fetch product with id: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(productById);
        }
        log.info("Fetched product with id: " + id);
        return ResponseEntity.ok(productById);
    }

    /**
     * API for updating a product.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Updates an existing product by id.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product updated.",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(value = "Successfully updated the product: <id>")
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Validation error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Malformed request or invalid input.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
    })
    public ResponseEntity<String> updateProduct(
        @Parameter(description = "Product identifier.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @PathVariable final String id,
        @Valid @RequestBody final ProductDto product) throws ProductNotFoundException {
        // Logic to update existing product
        service.updateProduct(id, product);
        log.info("Updated product to: " + product.toString());
        return ResponseEntity.ok("Successfully updated the product: " + id);
    }

    /**
     * API for deleting a product.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Deletes a product by id.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product deleted.",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(value = "Successfully deleted the product: <id>")
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
    })
    public ResponseEntity<String> deleteProduct(
        @Parameter(description = "Product identifier.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @PathVariable final String id) throws ProductNotFoundException {
        // Logic to delete product
        service.deleteProduct(id);
        log.info("Deleted product with id: " + id);
        return ResponseEntity.ok("Successfully deleted the product: " + id);
    }
}
