package uk.co.aosd.flash.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import uk.co.aosd.flash.domain.Product;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.exc.DuplicateProductException;
import uk.co.aosd.flash.exc.ProductNotFoundException;
import uk.co.aosd.flash.repository.ProductRepository;

@Service
@RequiredArgsConstructor
public class ProductsService {

    private final ProductRepository repository;

    private Function<Product, ProductDto> toProductDto = p -> {
        return new ProductDto(p.getId().toString(), p.getName(), p.getDescription(), p.getTotalPhysicalStock(), p.getBasePrice());
    };

    // CREATE
    @Transactional
    public UUID createProduct(@Valid final ProductDto productDto) throws DuplicateProductException {
        final Product product = new Product();
        product.setId(null);
        product.setName(productDto.name());
        product.setDescription(productDto.description());
        product.setTotalPhysicalStock(productDto.totalPhysicalStock());
        product.setBasePrice(productDto.basePrice());

        try {
            final var saved = repository.save(product);
            return saved.getId();
        } catch (final DuplicateKeyException e) {
            throw new DuplicateProductException(productDto.id(), productDto.name());
        }
    }

    // READ (All)
    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        return repository.findAll().stream().map(toProductDto).toList();
    }

    // READ (Single)
    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public Optional<ProductDto> getProductById(final String id) {
        try {
            return repository.findById(UUID.fromString(id)).map(toProductDto);
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // UPDATE
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void updateProduct(final String id, @Valid final ProductDto product) throws ProductNotFoundException {
        try {
            final var p = repository.findById(UUID.fromString(id));
            if (p.isEmpty()) {
                throw new ProductNotFoundException(id);
            }

            p.ifPresent(prod -> {
                prod.setName(product.name());
                prod.setDescription(product.description());
                prod.setTotalPhysicalStock(product.totalPhysicalStock());
                prod.setBasePrice(product.basePrice());
                repository.save(prod);
            });
        } catch (final IllegalArgumentException e) {
            throw new ProductNotFoundException(id);
        }
    }

    // DELETE
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(final String id) throws ProductNotFoundException {
        try {
            repository.deleteById(UUID.fromString(id));
        } catch (final IllegalArgumentException e) {
            throw new ProductNotFoundException(id);
        }
    }

}
