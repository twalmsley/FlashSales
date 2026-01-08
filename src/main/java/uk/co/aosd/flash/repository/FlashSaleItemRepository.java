package uk.co.aosd.flash.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import uk.co.aosd.flash.domain.FlashSaleItem;

@Repository
public interface FlashSaleItemRepository extends JpaRepository<FlashSaleItem, UUID> {

    @Modifying
    @Transactional
    @Query("UPDATE FlashSaleItem f SET f.soldCount = f.soldCount + :increment " +
           "WHERE f.id = :id AND f.soldCount + :increment <= f.allocatedStock")
    int incrementSoldCount(@Param("id") UUID id, @Param("increment") int increment);
}
