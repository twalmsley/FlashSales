package uk.co.aosd.flash.repository;

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
     * @param id the order ID
     * @return the order with flash sale item
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.flashSaleItem fsi LEFT JOIN FETCH fsi.flashSale WHERE o.id = :id")
    Optional<Order> findByIdWithFlashSaleItem(@Param("id") UUID id);

    /**
     * Find order by ID with product eagerly loaded.
     *
     * @param id the order ID
     * @return the order with product
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.product WHERE o.id = :id")
    Optional<Order> findByIdWithProduct(@Param("id") UUID id);

    /**
     * Find order by ID and userId with all related entities eagerly loaded.
     * Used for ownership validation and retrieving complete order details.
     *
     * @param id the order ID
     * @param userId the user ID
     * @return the order with all related entities
     */
    @Query("SELECT o FROM Order o " +
          "LEFT JOIN FETCH o.product p " +
          "LEFT JOIN FETCH o.flashSaleItem fsi " +
          "LEFT JOIN FETCH fsi.flashSale fs " +
          "WHERE o.id = :id AND o.userId = :userId")
    Optional<Order> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    /**
     * Find all orders for a user with all related entities eagerly loaded.
     * Results are ordered by createdAt descending (most recent first).
     *
     * @param userId the user ID
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
     * @param userId the user ID
     * @param status the order status
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
     * @param userId the user ID
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
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
     * Find orders by user, status, and date range with all related entities eagerly loaded.
     * Results are ordered by createdAt descending (most recent first).
     *
     * @param userId the user ID
     * @param status the order status
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
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
     * @param status optional status filter
     * @param startDate optional start date filter (inclusive)
     * @param endDate optional end date filter (inclusive)
     * @param userId optional user ID filter
     * @return list of orders matching the criteria
     */
    @Query("SELECT DISTINCT o FROM Order o " +
          "LEFT JOIN FETCH o.product p " +
          "LEFT JOIN FETCH o.flashSaleItem fsi " +
          "LEFT JOIN FETCH fsi.flashSale fs " +
          "WHERE (:status IS NULL OR o.status = :status) " +
          "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
          "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
          "AND (:userId IS NULL OR o.userId = :userId) " +
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
     * @param id the order ID
     * @return the order with all related entities
     */
    @Query("SELECT o FROM Order o " +
          "LEFT JOIN FETCH o.product p " +
          "LEFT JOIN FETCH o.flashSaleItem fsi " +
          "LEFT JOIN FETCH fsi.flashSale fs " +
          "WHERE o.id = :id")
    Optional<Order> findByIdForAdmin(@Param("id") UUID id);
}
