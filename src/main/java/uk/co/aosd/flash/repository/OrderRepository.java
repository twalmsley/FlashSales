package uk.co.aosd.flash.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.co.aosd.flash.domain.Order;
import uk.co.aosd.flash.domain.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Find order by ID with flash sale item eagerly loaded.
     *
     * @param id
     *            the order ID
     * @return the order with flash sale item
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.flashSaleItem fsi LEFT JOIN FETCH fsi.flashSale WHERE o.id = :id")
    Optional<Order> findByIdWithFlashSaleItem(@Param("id") UUID id);

    /**
     * Find order by ID with product eagerly loaded.
     *
     * @param id
     *            the order ID
     * @return the order with product
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.product WHERE o.id = :id")
    Optional<Order> findByIdWithProduct(@Param("id") UUID id);

    /**
     * Find order by ID and userId with all related entities eagerly loaded.
     * Used for ownership validation and retrieving complete order details.
     *
     * @param id
     *            the order ID
     * @param userId
     *            the user ID
     * @return the order with all related entities
     */
    @Query("SELECT o FROM Order o " +
        "LEFT JOIN FETCH o.product p " +
        "LEFT JOIN FETCH o.flashSaleItem fsi " +
        "LEFT JOIN FETCH fsi.flashSale fs " +
        "WHERE o.id = :id AND o.userId = :userId")
    Optional<Order> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    /**
     * Find order by user and flash sale item with all related entities eagerly loaded.
     * Used to check if the user already has an order for a given sale on the sale detail page.
     *
     * @param userId
     *            the user ID
     * @param flashSaleItemId
     *            the flash sale item ID
     * @return the order with product, flashSaleItem, and flashSale loaded
     */
    @Query("SELECT o FROM Order o " +
        "LEFT JOIN FETCH o.product p " +
        "LEFT JOIN FETCH o.flashSaleItem fsi " +
        "LEFT JOIN FETCH fsi.flashSale fs " +
        "WHERE o.userId = :userId AND o.flashSaleItem.id = :flashSaleItemId")
    Optional<Order> findByUserIdAndFlashSaleItemId(
        @Param("userId") UUID userId,
        @Param("flashSaleItemId") UUID flashSaleItemId);

    /**
     * Find all orders for a user with all related entities eagerly loaded.
     * Results are ordered by createdAt descending (most recent first).
     *
     * @param userId
     *            the user ID
     * @return list of orders for the user
     */
    @Query("SELECT o FROM Order o " +
        "LEFT JOIN FETCH o.product p " +
        "LEFT JOIN FETCH o.flashSaleItem fsi " +
        "LEFT JOIN FETCH fsi.flashSale fs " +
        "WHERE o.userId = :userId " +
        "ORDER BY o.createdAt DESC")
    List<Order> findByUserId(@Param("userId") UUID userId);

    /**
     * Find orders by user and status with all related entities eagerly loaded.
     * Results are ordered by createdAt descending (most recent first).
     *
     * @param userId
     *            the user ID
     * @param status
     *            the order status
     * @return list of orders matching the criteria
     */
    @Query("SELECT o FROM Order o " +
        "LEFT JOIN FETCH o.product p " +
        "LEFT JOIN FETCH o.flashSaleItem fsi " +
        "LEFT JOIN FETCH fsi.flashSale fs " +
        "WHERE o.userId = :userId AND o.status = :status " +
        "ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") OrderStatus status);

    /**
     * Find orders by user and date range with all related entities eagerly loaded.
     * Results are ordered by createdAt descending (most recent first).
     *
     * @param userId
     *            the user ID
     * @param startDate
     *            the start date (inclusive)
     * @param endDate
     *            the end date (inclusive)
     * @return list of orders matching the criteria
     */
    @Query("SELECT o FROM Order o " +
        "LEFT JOIN FETCH o.product p " +
        "LEFT JOIN FETCH o.flashSaleItem fsi " +
        "LEFT JOIN FETCH fsi.flashSale fs " +
        "WHERE o.userId = :userId AND o.createdAt >= :startDate AND o.createdAt <= :endDate " +
        "ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndCreatedAtBetween(
        @Param("userId") UUID userId,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate);

    /**
     * Find orders by user, status, and date range with all related entities eagerly
     * loaded.
     * Results are ordered by createdAt descending (most recent first).
     *
     * @param userId
     *            the user ID
     * @param status
     *            the order status
     * @param startDate
     *            the start date (inclusive)
     * @param endDate
     *            the end date (inclusive)
     * @return list of orders matching the criteria
     */
    @Query("SELECT o FROM Order o " +
        "LEFT JOIN FETCH o.product p " +
        "LEFT JOIN FETCH o.flashSaleItem fsi " +
        "LEFT JOIN FETCH fsi.flashSale fs " +
        "WHERE o.userId = :userId AND o.status = :status " +
        "AND o.createdAt >= :startDate AND o.createdAt <= :endDate " +
        "ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndStatusAndCreatedAtBetween(
        @Param("userId") UUID userId,
        @Param("status") OrderStatus status,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate);

    /**
     * Find all orders with optional filters for admin use.
     * All related entities (product, flashSaleItem, flashSale) are eagerly loaded.
     * Results are ordered by createdAt descending (most recent first).
     *
     * @param status
     *            optional status filter
     * @param startDate
     *            optional start date filter (inclusive)
     * @param endDate
     *            optional end date filter (inclusive)
     * @param userId
     *            optional user ID filter
     * @return list of orders matching the criteria
     */
    @Query("SELECT DISTINCT o FROM Order o " +
        "LEFT JOIN FETCH o.product p " +
        "LEFT JOIN FETCH o.flashSaleItem fsi " +
        "LEFT JOIN FETCH fsi.flashSale fs " +
        "WHERE o.status = COALESCE(:status, o.status) " +
        "AND o.createdAt >= COALESCE(:startDate, o.createdAt) " +
        "AND o.createdAt <= COALESCE(:endDate, o.createdAt) " +
        "AND o.userId = COALESCE(:userId, o.userId) " +
        "ORDER BY o.createdAt DESC")
    List<Order> findAllWithFilters(
        @Param("status") OrderStatus status,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate,
        @Param("userId") UUID userId);

    /**
     * Find order by ID with all related entities eagerly loaded for admin view.
     * Does not validate user ownership.
     *
     * @param id
     *            the order ID
     * @return the order with all related entities
     */
    @Query("SELECT o FROM Order o " +
        "LEFT JOIN FETCH o.product p " +
        "LEFT JOIN FETCH o.flashSaleItem fsi " +
        "LEFT JOIN FETCH fsi.flashSale fs " +
        "WHERE o.id = :id")
    Optional<Order> findByIdForAdmin(@Param("id") UUID id);

    /**
     * Calculate total revenue from orders with optional status and date range
     * filters.
     * Revenue is calculated as SUM(soldPrice * soldQuantity) for matching orders.
     *
     * @param status
     *            optional status filter (null for all statuses)
     * @param startDate
     *            optional start date filter (null for no lower bound)
     * @param endDate
     *            optional end date filter (null for no upper bound)
     * @return total revenue, or 0 if no matching orders
     */
    @Query("SELECT COALESCE(SUM(o.soldPrice * o.soldQuantity), 0) FROM Order o " +
        "WHERE o.status = COALESCE(:status, o.status) " +
        "AND o.createdAt >= COALESCE(:startDate, o.createdAt) " +
        "AND o.createdAt <= COALESCE(:endDate, o.createdAt)")
    BigDecimal calculateTotalRevenue(
        @Param("status") OrderStatus status,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate);

    /**
     * Count orders by status with optional date range filter.
     *
     * @param status
     *            the order status
     * @param startDate
     *            optional start date filter (null for no lower bound)
     * @param endDate
     *            optional end date filter (null for no upper bound)
     * @return count of orders matching the criteria
     */
    @Query("SELECT COUNT(o) FROM Order o " +
        "WHERE o.status = :status " +
        "AND o.createdAt >= COALESCE(:startDate, o.createdAt) " +
        "AND o.createdAt <= COALESCE(:endDate, o.createdAt)")
    Long countOrdersByStatus(
        @Param("status") OrderStatus status,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate);

    /**
     * Calculate average order value (total revenue / number of paid orders).
     *
     * @param startDate
     *            optional start date filter (null for no lower bound)
     * @param endDate
     *            optional end date filter (null for no upper bound)
     * @return average order value, or 0 if no paid orders
     */
    @Query("SELECT COALESCE(SUM(o.soldPrice * o.soldQuantity) / NULLIF(COUNT(o), 0), 0) FROM Order o " +
        "WHERE o.status = 'PAID' " +
        "AND o.createdAt >= COALESCE(:startDate, o.createdAt) " +
        "AND o.createdAt <= COALESCE(:endDate, o.createdAt)")
    BigDecimal calculateAverageOrderValue(
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate);

    /**
     * Find top products by revenue (sum of soldPrice * soldQuantity) for paid
     * orders.
     *
     * @param limit
     *            maximum number of results
     * @param startDate
     *            optional start date filter (null for no lower bound)
     * @param endDate
     *            optional end date filter (null for no upper bound)
     * @return list of Object arrays: [productId, productName, revenue]
     */
    @Query(value = "SELECT o.product_id, p.name, SUM(o.sold_price * o.sold_quantity) as revenue " +
        "FROM orders o " +
        "JOIN products p ON o.product_id = p.id " +
        "WHERE o.status = 'PAID' " +
        "AND o.created_at >= COALESCE(:startDate, TIMESTAMP '1970-01-01 00:00:00+00') " +
        "AND o.created_at <= COALESCE(:endDate, TIMESTAMP '9999-12-31 23:59:59+00') " +
        "GROUP BY o.product_id, p.name " +
        "ORDER BY revenue DESC " +
        "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopProductsByRevenue(
        @Param("limit") int limit,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate);

    /**
     * Find top products by quantity sold for paid orders.
     *
     * @param limit
     *            maximum number of results
     * @param startDate
     *            optional start date filter (null for no lower bound)
     * @param endDate
     *            optional end date filter (null for no upper bound)
     * @return list of Object arrays: [productId, productName, quantitySold]
     */
    @Query(value = "SELECT o.product_id, p.name, SUM(o.sold_quantity) as quantity_sold " +
        "FROM orders o " +
        "JOIN products p ON o.product_id = p.id " +
        "WHERE o.status = 'PAID' " +
        "AND o.created_at >= COALESCE(:startDate, TIMESTAMP '1970-01-01 00:00:00+00') " +
        "AND o.created_at <= COALESCE(:endDate, TIMESTAMP '9999-12-31 23:59:59+00') " +
        "GROUP BY o.product_id, p.name " +
        "ORDER BY quantity_sold DESC " +
        "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopProductsByQuantity(
        @Param("limit") int limit,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate);

    /**
     * Calculate total order quantity (sum of soldQuantity) with optional filters.
     *
     * @param startDate
     *            optional start date filter (null for no lower bound)
     * @param endDate
     *            optional end date filter (null for no upper bound)
     * @return total quantity ordered
     */
    @Query("SELECT COALESCE(SUM(o.soldQuantity), 0) FROM Order o " +
        "WHERE o.createdAt >= COALESCE(:startDate, o.createdAt) " +
        "AND o.createdAt <= COALESCE(:endDate, o.createdAt)")
    Long calculateTotalOrderQuantity(
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate);

    /**
     * Calculate revenue for a specific product from paid orders.
     *
     * @param productId
     *            the product ID
     * @param startDate
     *            optional start date filter (null for no lower bound)
     * @param endDate
     *            optional end date filter (null for no upper bound)
     * @return total revenue for the product
     */
    @Query("SELECT COALESCE(SUM(o.soldPrice * o.soldQuantity), 0) FROM Order o " +
        "WHERE o.product.id = :productId AND o.status = 'PAID' " +
        "AND o.createdAt >= COALESCE(:startDate, o.createdAt) " +
        "AND o.createdAt <= COALESCE(:endDate, o.createdAt)")
    BigDecimal calculateRevenueForProduct(
        @Param("productId") UUID productId,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate);

    /**
     * Calculate quantity sold for a specific product from paid orders.
     *
     * @param productId
     *            the product ID
     * @param startDate
     *            optional start date filter (null for no lower bound)
     * @param endDate
     *            optional end date filter (null for no upper bound)
     * @return total quantity sold for the product
     */
    @Query("SELECT COALESCE(SUM(o.soldQuantity), 0) FROM Order o " +
        "WHERE o.product.id = :productId AND o.status = 'PAID' " +
        "AND o.createdAt >= COALESCE(:startDate, o.createdAt) " +
        "AND o.createdAt <= COALESCE(:endDate, o.createdAt)")
    Long calculateQuantityForProduct(
        @Param("productId") UUID productId,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate);
}
