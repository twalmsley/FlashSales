package uk.co.aosd.flash.services;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.exc.DuplicateProductException;
import uk.co.aosd.flash.exc.ProductNotFoundException;

@Service
public class ProductsService {

    private static Logger log = LoggerFactory.getLogger(ProductsService.class.getName());

    // CREATE
    @Transactional
    public void createProduct(@Valid final ProductDto product) throws DuplicateProductException {
        log.trace("createProduct():" + product.toString());
    }

    // READ (All)
    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        log.trace("getAllProducts()");
        return List.of();
    }

    // READ (Single)
    @Transactional(readOnly = true)
    public Optional<ProductDto> getProductById(final String id) {
        log.trace("getProductById(): " + id);
        return Optional.empty();
    }

    // UPDATE
    @Transactional
    public void updateProduct(final String id, @Valid final ProductDto product) throws ProductNotFoundException {
        log.trace("updateProduct():" + product.toString());
    }

    // DELETE
    @Transactional
    public void deleteProduct(final String id) throws ProductNotFoundException {
        log.trace("deleteProduct(): " + id);
    }

}
