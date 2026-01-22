package uk.co.aosd.flash.controllers;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.co.aosd.flash.dto.ClientProductDto;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.services.ProductsService;

/**
 * Client API.
 */
@RestController
@Profile("api-service")
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientRestApi {

    private static Logger log = LoggerFactory.getLogger(ClientRestApi.class.getName());

    private final ProductsService service;

    /**
     * Get a client's view of a product.
     *
     * @param id Product ID String
     * @return maybe a ClientProductDto
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<Optional<ClientProductDto>> getProductById(@PathVariable final String id) {
        // Logic to return a single product by ID
        final Optional<ProductDto> productById = service.getProductById(id);
        if (productById.isEmpty()) {
            log.info("Failed to fetch product with id: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Optional.empty());
        }
        log.info("Fetched product with id: " + id);
        final ProductDto dto = productById.get();
        final ClientProductDto clientProductById = new ClientProductDto(id, dto.name(), dto.description(), dto.basePrice());
        return ResponseEntity.ok(Optional.of(clientProductById));
    }

}
