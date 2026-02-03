package uk.co.aosd.flash.repository;

import java.math.BigDecimal;
import java.util.List;
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

    /**
     * Increment both total physical stock and reserved count for a product.
     * Used for reverse transitions (e.g., DISPATCHED → PAID).
     *
     * @param id       the product ID
     * @param quantity the quantity to increment
     * @return the number of rows updated
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Product p SET p.totalPhysicalStock = p.totalPhysicalStock + :quantity, "
        + "p.reservedCount = p.reservedCount + :quantity "
        + "WHERE p.id = :id")
    int incrementStock(@Param("id") UUID id, @Param("quantity") int quantity);

    /**
     * Count total number of products in catalog.
     *
     * @return total product count
     */
    long count();

    /**
     * Calculate total physical stock across all products.
     *
     * @return total physical stock
     */
    @Query("SELECT COALESCE(SUM(p.totalPhysicalStock), 0) FROM Product p")
    Long calculateTotalPhysicalStock();

    /**
     * Calculate total reserved stock across all products.
     *
     * @return total reserved stock
     */
    @Query("SELECT COALESCE(SUM(p.reservedCount), 0) FROM Product p")
    Long calculateTotalReservedStock();

    /**
     * Find products with low stock (available stock below threshold).
     * Available stock = totalPhysicalStock - reservedCount.
     *
     * @param threshold the stock threshold (products with available stock <= threshold are returned)
     * @return list of products with low stock
     */
    @Query("SELECT p FROM Product p WHERE (p.totalPhysicalStock - p.reservedCount) <= :threshold")
    List<Product> findProductsWithLowStock(@Param("threshold") Integer threshold);

    /**
     * Find products with optional full-text search (name + description) and optional price range.
     * When search is null or blank, no FTS filter is applied. When minPrice/maxPrice are null,
     * no price filter is applied. Uses PostgreSQL full-text search (plainto_tsquery) for safety.
     *
     * @param search  optional search term (null or blank = no search)
     * @param minPrice optional minimum base price (inclusive)
     * @param maxPrice optional maximum base price (inclusive)
     * @return list of products matching the criteria
     */
    @Query(value = "SELECT p.id, p.name, p.description, p.base_price, p.total_physical_stock, p.reserved_count " +
        "FROM products p " +
        "WHERE (CAST(:search AS TEXT) IS NULL OR trim(CAST(:search AS TEXT)) = '' OR p.search_vector @@ plainto_tsquery('english', COALESCE(trim(CAST(:search AS TEXT)), ''))) " +
        "AND (CAST(:minPrice AS DECIMAL(12,2)) IS NULL OR p.base_price >= CAST(:minPrice AS DECIMAL(12,2))) " +
        "AND (CAST(:maxPrice AS DECIMAL(12,2)) IS NULL OR p.base_price <= CAST(:maxPrice AS DECIMAL(12,2)))",
        nativeQuery = true)
    List<Product> findAllWithSearchAndPrice(
        @Param("search") String search,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice);
}
