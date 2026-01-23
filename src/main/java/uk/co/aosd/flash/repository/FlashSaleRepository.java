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
     * Find DRAFT sales with start time within the next N days from the given current time.
     * Uses JOIN FETCH to eagerly load items and products to avoid lazy loading issues.
     *
     * @param status the sale status (should be DRAFT)
     * @param currentTime the current time
     * @param futureTime the future time (currentTime + N days)
     * @return list of draft sales within the time range
     */
    @Query("SELECT DISTINCT fs FROM FlashSale fs LEFT JOIN FETCH fs.items item LEFT JOIN FETCH item.product WHERE fs.status = :status AND fs.startTime >= :currentTime AND fs.startTime <= :futureTime ORDER BY fs.startTime ASC")
    List<FlashSale> findDraftSalesWithinDays(@Param("status") SaleStatus status, @Param("currentTime") OffsetDateTime currentTime, @Param("futureTime") OffsetDateTime futureTime);

    /**
     * Find DRAFT sales that are ready to be activated (start time has passed).
     * Uses JOIN FETCH to eagerly load items and products to avoid lazy loading issues.
     *
     * @param status the sale status (should be DRAFT)
     * @param currentTime the current time
     * @return list of draft sales ready to activate
     */
    @Query("SELECT DISTINCT fs FROM FlashSale fs LEFT JOIN FETCH fs.items item LEFT JOIN FETCH item.product WHERE fs.status = :status AND fs.startTime <= :currentTime ORDER BY fs.startTime ASC")
    List<FlashSale> findDraftSalesReadyToActivate(@Param("status") SaleStatus status, @Param("currentTime") OffsetDateTime currentTime);

    /**
     * Find ACTIVE sales that are ready to be completed (end time has passed).
     * Uses JOIN FETCH to eagerly load items and products to avoid lazy loading issues.
     *
     * @param status the sale status (should be ACTIVE)
     * @param currentTime the current time
     * @return list of active sales ready to complete
     */
    @Query("SELECT DISTINCT fs FROM FlashSale fs LEFT JOIN FETCH fs.items item LEFT JOIN FETCH item.product WHERE fs.status = :status AND fs.endTime <= :currentTime ORDER BY fs.endTime ASC")
    List<FlashSale> findActiveSalesReadyToComplete(@Param("status") SaleStatus status, @Param("currentTime") OffsetDateTime currentTime);
}
