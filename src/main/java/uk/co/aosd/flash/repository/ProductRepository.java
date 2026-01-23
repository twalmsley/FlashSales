package uk.co.aosd.flash.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import uk.co.aosd.flash.domain.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * Decrement both total physical stock and reserved count for a product.
     *
     * @param id       the product ID
     * @param quantity the quantity to decrement
     * @return the number of rows updated
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Product p SET p.totalPhysicalStock = p.totalPhysicalStock - :quantity, "
        + "p.reservedCount = p.reservedCount - :quantity "
        + "WHERE p.id = :id AND p.totalPhysicalStock >= :quantity AND p.reservedCount >= :quantity")
    int decrementStock(@Param("id") UUID id, @Param("quantity") int quantity);
}
