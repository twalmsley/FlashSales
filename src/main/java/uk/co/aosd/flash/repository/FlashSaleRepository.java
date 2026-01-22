package uk.co.aosd.flash.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.aosd.flash.domain.FlashSale;
import uk.co.aosd.flash.domain.SaleStatus;

/**
 * Flash Sale Repository.
 */
@Repository
public interface FlashSaleRepository extends JpaRepository<FlashSale, UUID> {
    List<FlashSale> findByStatus(SaleStatus status);
}
