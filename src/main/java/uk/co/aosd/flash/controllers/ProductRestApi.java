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
    public ResponseEntity<String> createProduct(@Valid @RequestBody ProductDto product) {
        log.trace("createProduct():" + product.toString());
        // Logic to save product
        try {
            service.createProduct(product);
        } catch (DuplicateProductException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(product.id());
        }
        return ResponseEntity.ok("Successfully created the product: " + product.id());
    }

    // READ (All)
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        log.trace("getAllProducts()");
        // Logic to return all products
        return ResponseEntity.ok(service.getAllProducts());
    }

    // READ (Single)
    @GetMapping("/{id}")
    public ResponseEntity<Optional<ProductDto>> getProductById(@PathVariable String id) {
        log.trace("getProductById(): " + id);
        // Logic to return a single product by ID
        final Optional<ProductDto> productById = service.getProductById(id);
        if (productById.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(productById);
        }
        return ResponseEntity.ok(productById);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<String> updateProduct(@PathVariable String id, @Valid @RequestBody ProductDto product) {
        log.trace("updateProduct():" + product.toString());
        // Logic to update existing product
        try {
            service.updateProduct(id, product);
        } catch (final ProductNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(id);
        }
        return ResponseEntity.ok("Successfully updated the product: " + id);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable String id) {
        log.trace("deleteProduct(): " + id);
        // Logic to delete product
        try {
            service.deleteProduct(id);
        } catch (final ProductNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(id);
        }
        return ResponseEntity.ok("Successfully deleted the product: " + id);
    }
}
