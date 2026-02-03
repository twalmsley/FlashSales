package uk.co.aosd.flash.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.co.aosd.flash.domain.FlashSale;
import uk.co.aosd.flash.domain.SaleStatus;

/**
 * Flash Sale Repository.
 */
@Repository
public interface FlashSaleRepository extends JpaRepository<FlashSale, UUID> {
    List<FlashSale> findByStatus(SaleStatus status);

    /**
     * Find DRAFT sales with start time within the next N days from the given
     * current time.
     * Uses JOIN FETCH to eagerly load items and products to avoid lazy loading
     * issues.
     *
     * @param status
     *            the sale status (should be DRAFT)
     * @param currentTime
     *            the current time
     * @param futureTime
     *            the future time (currentTime + N days)
     * @return list of draft sales within the time range
     */
    @Query("SELECT DISTINCT fs FROM FlashSale fs LEFT JOIN FETCH fs.items item LEFT JOIN FETCH item.product WHERE fs.status = :status AND fs.startTime >= :currentTime AND fs.startTime <= :futureTime ORDER BY fs.startTime ASC")
    List<FlashSale> findDraftSalesWithinDays(@Param("status") SaleStatus status, @Param("currentTime") OffsetDateTime currentTime,
        @Param("futureTime") OffsetDateTime futureTime);

    /**
     * Find DRAFT sales that are ready to be activated (start time has passed).
     * Uses JOIN FETCH to eagerly load items and products to avoid lazy loading
     * issues.
     *
     * @param status
     *            the sale status (should be DRAFT)
     * @param currentTime
     *            the current time
     * @return list of draft sales ready to activate
     */
    @Query("SELECT DISTINCT fs FROM FlashSale fs LEFT JOIN FETCH fs.items item LEFT JOIN FETCH item.product WHERE fs.status = :status AND fs.startTime <= :currentTime ORDER BY fs.startTime ASC")
    List<FlashSale> findDraftSalesReadyToActivate(@Param("status") SaleStatus status, @Param("currentTime") OffsetDateTime currentTime);

    /**
     * Find ACTIVE sales that are ready to be completed (end time has passed).
     * Uses JOIN FETCH to eagerly load items and products to avoid lazy loading
     * issues.
     *
     * @param status
     *            the sale status (should be ACTIVE)
     * @param currentTime
     *            the current time
     * @return list of active sales ready to complete
     */
    @Query("SELECT DISTINCT fs FROM FlashSale fs LEFT JOIN FETCH fs.items item LEFT JOIN FETCH item.product WHERE fs.status = :status AND fs.endTime <= :currentTime ORDER BY fs.endTime ASC")
    List<FlashSale> findActiveSalesReadyToComplete(@Param("status") SaleStatus status, @Param("currentTime") OffsetDateTime currentTime);

    /**
     * Find all flash sales with optional filters for status and date range.
     * Uses JOIN FETCH to eagerly load items and products to avoid lazy loading
     * issues.
     *
     * @param status
     *            optional status filter
     * @param startDate
     *            optional filter window start (inclusive). When provided, only
     *            sales whose time period overlaps
     *            the specified window are returned.
     * @param endDate
     *            optional filter window end (inclusive). When provided, only sales
     *            whose time period overlaps
     *            the specified window are returned.
     * @return list of flash sales matching the filters, ordered by startTime
     */
    @Query("SELECT DISTINCT fs FROM FlashSale fs LEFT JOIN FETCH fs.items item LEFT JOIN FETCH item.product " +
        "WHERE fs.status = COALESCE(:status, fs.status) " +
        // overlap: saleEnd >= filterStart AND saleStart <= filterEnd (with open-ended
        // window support)
        "AND fs.endTime >= COALESCE(:startDate, fs.endTime) " +
        "AND fs.startTime <= COALESCE(:endDate, fs.startTime) " +
        "ORDER BY fs.startTime ASC")
    List<FlashSale> findAllWithFilters(
        @Param("status") SaleStatus status,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate);

    /**
     * Find flash sale IDs with optional filters and optional title search (FTS).
     * Used when search is non-blank; results are then loaded with findByIdInWithItems.
     *
     * @param status    optional status filter
     * @param startDate optional filter window start
     * @param endDate   optional filter window end
     * @param search    optional search term for title (null or blank = no FTS)
     * @return list of flash sale IDs in start_time order
     */
    @Query(value = "SELECT id FROM flash_sales fs " +
        "WHERE (cast(:status AS text) IS NULL OR fs.status::text = cast(:status AS text)) " +
        "AND fs.end_time >= COALESCE(:startDate, fs.end_time) " +
        "AND fs.start_time <= COALESCE(:endDate, fs.start_time) " +
        "AND (:search IS NULL OR trim(cast(:search AS text)) = '' OR fs.search_vector @@ plainto_tsquery('english', :search)) " +
        "ORDER BY fs.start_time ASC",
        nativeQuery = true)
    List<UUID> findFlashSaleIdsWithFiltersAndSearch(
        @Param("status") SaleStatus status,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate,
        @Param("search") String search);

    /**
     * Find flash sales by IDs with items and products eagerly loaded.
     * Does not guarantee order; caller should sort by startTime if needed.
     *
     * @param ids list of flash sale IDs
     * @return list of flash sales with items and products loaded
     */
    @Query("SELECT DISTINCT fs FROM FlashSale fs LEFT JOIN FETCH fs.items item LEFT JOIN FETCH item.product WHERE fs.id IN :ids")
    List<FlashSale> findByIdInWithItems(@Param("ids") List<UUID> ids);

    /**
     * Find a flash sale by ID with items and products eagerly loaded.
     * Uses JOIN FETCH to eagerly load items and products to avoid lazy loading
     * issues.
     *
     * @param id
     *            the flash sale ID
     * @return optional flash sale with items and products loaded
     */
    @Query("SELECT DISTINCT fs FROM FlashSale fs LEFT JOIN FETCH fs.items item LEFT JOIN FETCH item.product WHERE fs.id = :id")
    java.util.Optional<FlashSale> findByIdWithItems(@Param("id") UUID id);

    /**
     * Count flash sales by status.
     *
     * @param status
     *            the sale status
     * @return count of sales with the given status
     */
    Long countByStatus(SaleStatus status);

    /**
     * Calculate total items sold across all flash sale items with optional date
     * range filter.
     * Items sold are tracked in the sold_count field of flash_sale_items.
     *
     * @param startDate
     *            optional start date filter (null for no lower bound)
     * @param endDate
     *            optional end date filter (null for no upper bound)
     * @return total items sold
     */
    @Query(value = "SELECT COALESCE(SUM(fsi.sold_count), 0) " +
        "FROM flash_sale_items fsi " +
        "JOIN flash_sales fs ON fs.id = fsi.flash_sale_id " +
        "WHERE fs.start_time >= COALESCE(:startDate, TIMESTAMP '1970-01-01 00:00:00+00') " +
        "AND fs.end_time <= COALESCE(:endDate, TIMESTAMP '9999-12-31 23:59:59+00')", nativeQuery = true)
    Long calculateTotalItemsSold(
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate);

    /**
     * Calculate total items allocated across all flash sale items with optional
     * date range filter.
     *
     * @param startDate
     *            optional start date filter (null for no lower bound)
     * @param endDate
     *            optional end date filter (null for no upper bound)
     * @return total items allocated
     */
    @Query(value = "SELECT COALESCE(SUM(fsi.allocated_stock), 0) " +
        "FROM flash_sale_items fsi " +
        "JOIN flash_sales fs ON fs.id = fsi.flash_sale_id " +
        "WHERE fs.start_time >= COALESCE(:startDate, TIMESTAMP '1970-01-01 00:00:00+00') " +
        "AND fs.end_time <= COALESCE(:endDate, TIMESTAMP '9999-12-31 23:59:59+00')", nativeQuery = true)
    Long calculateTotalItemsAllocated(
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate);

    /**
     * Find top sales by items sold with optional date range filter.
     *
     * @param limit
     *            maximum number of results
     * @param startDate
     *            optional start date filter (null for no lower bound)
     * @param endDate
     *            optional end date filter (null for no upper bound)
     * @return list of Object arrays: [saleId, title, itemsSold, revenue]
     */
    @Query(value = "SELECT fs.id, fs.title, SUM(fsi.sold_count) as items_sold, " +
        "SUM(fsi.sold_count * fsi.sale_price) as revenue " +
        "FROM flash_sales fs " +
        "JOIN flash_sale_items fsi ON fs.id = fsi.flash_sale_id " +
        "WHERE fs.start_time >= COALESCE(:startDate, TIMESTAMP '1970-01-01 00:00:00+00') " +
        "AND fs.end_time <= COALESCE(:endDate, TIMESTAMP '9999-12-31 23:59:59+00') " +
        "GROUP BY fs.id, fs.title " +
        "ORDER BY items_sold DESC " +
        "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopSalesByItemsSold(
        @Param("limit") int limit,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate);

    /**
     * Calculate average number of products per sale.
     * This counts the number of flash sale items (products) per sale and averages
     * across all sales.
     *
     * @return average products per sale
     */
    @Query(value = "SELECT COALESCE(AVG(product_count), 0) " +
        "FROM (SELECT fs.id, COUNT(fsi.id) as product_count " +
        "      FROM flash_sales fs " +
        "      LEFT JOIN flash_sale_items fsi ON fs.id = fsi.flash_sale_id " +
        "      GROUP BY fs.id) as sale_product_counts", nativeQuery = true)
    Double calculateAverageProductsPerSale();
}
