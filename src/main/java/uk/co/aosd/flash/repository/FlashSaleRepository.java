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
     *            optional filter window start (inclusive). When provided, only sales whose time period overlaps
     *            the specified window are returned.
     * @param endDate
     *            optional filter window end (inclusive). When provided, only sales whose time period overlaps
     *            the specified window are returned.
     * @return list of flash sales matching the filters, ordered by startTime
     */
    @Query("SELECT DISTINCT fs FROM FlashSale fs LEFT JOIN FETCH fs.items item LEFT JOIN FETCH item.product " +
        "WHERE fs.status = COALESCE(:status, fs.status) " +
        // overlap: saleEnd >= filterStart AND saleStart <= filterEnd (with open-ended window support)
        "AND fs.endTime >= COALESCE(:startDate, fs.endTime) " +
        "AND fs.startTime <= COALESCE(:endDate, fs.startTime) " +
        "ORDER BY fs.startTime ASC")
    List<FlashSale> findAllWithFilters(
        @Param("status") SaleStatus status,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate);

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
}
