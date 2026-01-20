package uk.co.aosd.flash.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.aosd.flash.domain.Product;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.exc.DuplicateEntityException;
import uk.co.aosd.flash.exc.ProductNotFoundException;
import uk.co.aosd.flash.repository.ProductRepository;

/**
 * A Service for managing Products.
 */
@Service
@RequiredArgsConstructor
public class ProductsService {

    private static Logger log = LoggerFactory.getLogger(ProductsService.class.getName());

    private final ProductRepository repository;

    private Function<Product, ProductDto> toProductDto = p -> {
        return new ProductDto(p.getId().toString(), p.getName(), p.getDescription(), p.getTotalPhysicalStock(), p.getBasePrice(), p.getReservedCount());
    };

    /**
     * Create a product.
     */
    @Transactional
    public UUID createProduct(@Valid final ProductDto productDto) throws DuplicateEntityException {
        log.info("Creating product: " + productDto);

        final Product product = new Product();
        product.setId(null);
        product.setName(productDto.name());
        product.setDescription(productDto.description());
        product.setTotalPhysicalStock(productDto.totalPhysicalStock());
        product.setBasePrice(productDto.basePrice());
        product.setReservedCount(productDto.reservedCount());

        try {
            final var saved = repository.save(product);
            return saved.getId();
        } catch (final DuplicateKeyException e) {
            log.error("Failed to create product: " + productDto);
            log.error("Failed to create product: " + e.getMessage());
            throw new DuplicateEntityException(productDto.id(), productDto.name());
        } catch (final ConstraintViolationException e) {
            log.error("Failed to create product: " + productDto);
            log.error("Failed to create product: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get all products.
     */
    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        log.info("Getting all products");
        return repository.findAll().stream().map(toProductDto).toList();
    }

    /**
     * Get a single product.
     */
    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public Optional<ProductDto> getProductById(final String id) {
        try {
            log.info("Getting product: " + id);
            final Optional<ProductDto> maybeProduct = repository.findById(UUID.fromString(id)).map(toProductDto);
            log.info("Got product: " + maybeProduct);
            return maybeProduct;
        } catch (final IllegalArgumentException e) {
            log.error("Failed to get product: " + id);
            log.error("Failed to get product: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Update a product.
     */
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void updateProduct(final String id, @Valid final ProductDto product) throws ProductNotFoundException {
        try {
            log.info("Updating product: " + id + ", " + product);
            final var p = repository.findById(UUID.fromString(id));
            if (p.isEmpty()) {
                log.info("Product not found: " + id);
                throw new ProductNotFoundException(id);
            }

            p.ifPresent(prod -> {
                prod.setName(product.name());
                prod.setDescription(product.description());
                prod.setTotalPhysicalStock(product.totalPhysicalStock());
                prod.setBasePrice(product.basePrice());
                prod.setReservedCount(product.reservedCount());
                repository.save(prod);
                log.info("Product Updated: " + prod);
            });
        } catch (final IllegalArgumentException e) {
            log.error("Failed to update product: " + product);
            log.error("Failed to update product: " + e.getMessage());
            throw new ProductNotFoundException(id);
        } catch (final ConstraintViolationException e) {
            log.error("Failed to update product: " + product);
            log.error("Failed to update product: " + e.getMessage());
            throw e;
        }

    }

    /**
     * Delete a product.
     */
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(final String id) throws ProductNotFoundException {
        try {
            log.info("Deleting product: " + id);
            repository.deleteById(UUID.fromString(id));
            log.info("Deleted product: " + id);
        } catch (final IllegalArgumentException e) {
            log.error("Failed to delete product: " + id);
            log.error("Failed to delete product: " + e.getMessage());
            throw new ProductNotFoundException(id);
        }
    }

}
