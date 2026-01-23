package uk.co.aosd.flash.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.co.aosd.flash.domain.FlashSaleItem;

/**
 * Repository for Flash Sale Items.
 */
@Repository
public interface FlashSaleItemRepository extends JpaRepository<FlashSaleItem, UUID> {

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE FlashSaleItem f SET f.soldCount = f.soldCount + :increment "
        + "WHERE f.id = :id AND f.soldCount + :increment <= f.allocatedStock "
        + "AND f.flashSale.status = 'ACTIVE'")
    int incrementSoldCount(@Param("id") UUID id, @Param("increment") int increment);

    /**
     * Decrement the sold count for a flash sale item.
     *
     * @param id        the flash sale item ID
     * @param decrement the amount to decrement
     * @return the number of rows updated
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE FlashSaleItem f SET f.soldCount = f.soldCount - :decrement "
        + "WHERE f.id = :id AND f.soldCount >= :decrement")
    int decrementSoldCount(@Param("id") UUID id, @Param("decrement") int decrement);

    /**
     * Find a flash sale item by its ID and flash sale ID.
     *
     * @param itemId     the flash sale item ID
     * @param flashSaleId the flash sale ID
     * @return optional flash sale item
     */
    @Query("SELECT f FROM FlashSaleItem f WHERE f.id = :itemId AND f.flashSale.id = :flashSaleId")
    Optional<FlashSaleItem> findByIdAndFlashSaleId(@Param("itemId") UUID itemId, @Param("flashSaleId") UUID flashSaleId);

    /**
     * Check if a flash sale item exists for a given flash sale and product.
     *
     * @param flashSaleId the flash sale ID
     * @param productId    the product ID
     * @return true if an item exists, false otherwise
     */
    boolean existsByFlashSaleIdAndProductId(UUID flashSaleId, UUID productId);
}
