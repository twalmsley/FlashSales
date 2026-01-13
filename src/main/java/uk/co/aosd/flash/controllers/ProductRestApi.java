package uk.co.aosd.flash.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.exc.DuplicateProductException;
import uk.co.aosd.flash.exc.ProductNotFoundException;
import uk.co.aosd.flash.services.ProductsService;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@Profile("admin-service")
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductRestApi {

    private static Logger log = LoggerFactory.getLogger(ProductRestApi.class.getName());

    private final ProductsService service;

    // CREATE
    @PostMapping
    public ResponseEntity<String> createProduct(@Valid @RequestBody final ProductDto product) {
        // Logic to save product
        try {
            service.createProduct(product);
            log.info("Created Product: " + product.toString());
        } catch (final DuplicateProductException e) {
            log.info("Failed to created Product due to duplicate entity: " + product.toString());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(product.id());
        }
        return ResponseEntity.created(URI.create("/api/v1/products/" + product.id())).build();
    }

    // READ (All)
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        log.info("Returned a list of all products.");
        // Logic to return all products
        return ResponseEntity.ok(service.getAllProducts());
    }

    // READ (Single)
    @GetMapping("/{id}")
    public ResponseEntity<Optional<ProductDto>> getProductById(@PathVariable final String id) {
        // Logic to return a single product by ID
        final Optional<ProductDto> productById = service.getProductById(id);
        if (productById.isEmpty()) {
            log.info("Failed to fetch product with id: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(productById);
        }
        log.info("Fetched product with id: " + id);
        return ResponseEntity.ok(productById);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<String> updateProduct(@PathVariable final String id, @Valid @RequestBody final ProductDto product) {
        // Logic to update existing product
        try {
            service.updateProduct(id, product);
            log.info("Updated product to: " + product.toString());
        } catch (final ProductNotFoundException e) {
            log.info("Failed to update product with id: " + product.toString() + " NOT FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(id);
        }
        return ResponseEntity.ok("Successfully updated the product: " + id);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable final String id) {
        // Logic to delete product
        try {
            service.deleteProduct(id);
            log.info("Deleted product with id: " + id);
        } catch (final ProductNotFoundException e) {

            log.info("Failed to delete product with id: " + id + " NOT FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(id);
        }
        return ResponseEntity.ok("Successfully deleted the product: " + id);
    }
}
