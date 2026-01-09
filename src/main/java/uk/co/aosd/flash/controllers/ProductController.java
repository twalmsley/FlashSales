package uk.co.aosd.flash.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import uk.co.aosd.flash.dto.ProductDto;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    // CREATE
    @PostMapping
    public ResponseEntity<String> createProduct(@Valid @RequestBody ProductDto product) {
        // Logic to save product
        return ResponseEntity.ok("Successfully created the product: " + product.id());
    }

    // READ (All)
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        // Logic to return all products
        return ResponseEntity.ok(
            List.of(
                new ProductDto(
                    "testid",
                    "Test Product Name",
                    "Test Product Desc",
                    Integer.valueOf(101),
                    BigDecimal.valueOf(109.99))));
    }

    // READ (Single)
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable String id) {
        // Logic to return a single product by ID
        return ResponseEntity.ok(
            new ProductDto(
                id,
                "Test Product Name",
                "Test Product Desc",
                Integer.valueOf(101),
                BigDecimal.valueOf(109.99)));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<String> updateProduct(@PathVariable String id, @Valid @RequestBody ProductDto product) {
        // Logic to update existing product
        return ResponseEntity.ok("Successfully updated the product: " + id);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable String id) {
        // Logic to delete product
        return ResponseEntity.ok("Successfully deleted the product: " + id);
    }
}
