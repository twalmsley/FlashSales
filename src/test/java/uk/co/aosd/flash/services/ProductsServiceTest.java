package uk.co.aosd.flash.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;

import uk.co.aosd.flash.domain.Product;
import uk.co.aosd.flash.dto.ProductDto;
import uk.co.aosd.flash.exc.DuplicateProductException;
import uk.co.aosd.flash.exc.ProductNotFoundException;
import uk.co.aosd.flash.repository.ProductRepository;

public class ProductsServiceTest {
    private static final String uuid1 = "547cf74d-7b64-44ea-b70f-cbcde09cadc9";
    private static final String uuid2 = "1c05690e-cd9a-42ee-9f15-194b4c454216";
    private static final String uuid3 = "ab3b715e-e2c2-4c28-925d-83ac93c32d02";

    private ProductRepository repository = Mockito.mock(ProductRepository.class);

    private ProductsService service = new ProductsService(repository);

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(repository);
    }

    @Test
    public void shouldSuccessfullyCreateAProduct() throws DuplicateProductException {
        final ProductDto product = new ProductDto(uuid1, "Test Product 1", "Description", 1, BigDecimal.valueOf(101.99));
        service.createProduct(product);
    }

    @Test
    public void shouldFailToCreateAProductWithDuplicateKey() throws DuplicateProductException {
        final ProductDto product = new ProductDto(uuid1, "Test Product 1", "Description", 1, BigDecimal.valueOf(101.99));

        service.createProduct(product);

        Mockito.doThrow(new DuplicateKeyException(uuid1)).when(repository).save(Mockito.any(Product.class));

        assertThrows(DuplicateProductException.class, () -> {
            service.createProduct(product);
        });
    }

    @Test
    public void shouldFindAllProductsEmpty() {
        final var products = service.getAllProducts();
        assertNotNull(products);
        assertTrue(products.isEmpty());
    }

    @Test
    public void shouldFindThreeProducts() {
        final Product seed1 = new Product(UUID.fromString(uuid1), "one", "desc1", 100, BigDecimal.valueOf(100.00));
        final Product seed2 = new Product(UUID.fromString(uuid2), "two", "desc2", 200, BigDecimal.valueOf(200.00));
        final Product seed3 = new Product(UUID.fromString(uuid3), "three", "desc3", 300, BigDecimal.valueOf(300.00));
        final List<Product> seed = List.of(seed1, seed2, seed3);

        Mockito.when(repository.findAll()).thenReturn(seed);
        final var products = service.getAllProducts();

        assertNotNull(products);
        assertEquals(3, products.size());
        assertEquals(1, products.stream().filter(p -> p.id().equals(uuid1)).count());
        assertEquals(1, products.stream().filter(p -> p.id().equals(uuid2)).count());
        assertEquals(1, products.stream().filter(p -> p.id().equals(uuid3)).count());
    }

    @Test
    public void shouldFindProductByIdSuccessfully() {
        final Product seed1 = new Product(UUID.fromString(uuid1), "one", "desc1", 100, BigDecimal.valueOf(100.00));

        final UUID uu = UUID.fromString(uuid1);
        Mockito.when(repository.findById(uu)).thenReturn(Optional.of(seed1));

        final var maybeProduct = service.getProductById(uuid1);

        assertFalse(maybeProduct.isEmpty());
        assertEquals(uuid1, maybeProduct.get().id());
    }

    @Test
    public void shouldFailToFindProductById() {
        final var maybeProduct = service.getProductById(uuid1);

        assertTrue(maybeProduct.isEmpty());
    }

    @Test
    public void shouldFailToFindProductByBadId() {
        final var maybeProduct = service.getProductById("bad uuid");

        assertTrue(maybeProduct.isEmpty());
    }

    @Test
    public void shouldSuccessfullyUpdateAProduct() throws ProductNotFoundException {
        final Product seed1 = new Product(UUID.fromString(uuid1), "one", "desc1", 100, BigDecimal.valueOf(100.00));

        final UUID uu = UUID.fromString(uuid1);
        Mockito.when(repository.findById(uu)).thenReturn(Optional.of(seed1));

        final ProductDto product = new ProductDto(uuid1, "Test Product 1", "Description", 1, BigDecimal.valueOf(101.99));
        service.updateProduct(uuid1, product);
    }

    @Test
    public void shouldNotFindProductToUpdate() throws ProductNotFoundException {
        final ProductDto product = new ProductDto(uuid2, "Test Product 2", "Description", 2, BigDecimal.valueOf(201.99));

        assertThrows(ProductNotFoundException.class, () -> {
            service.updateProduct(uuid2, product);
        });
    }

    @Test
    public void shouldDeleteByIdSuccessfully() throws ProductNotFoundException {
        service.deleteProduct(uuid1);
    }

    @Test
    public void shouldFailDeleteByIdDueToBadId() throws ProductNotFoundException {
        assertThrows(ProductNotFoundException.class, () -> {
            service.deleteProduct("bad uuid");
        });
    }
}
