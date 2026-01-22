package uk.co.aosd.flash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.aosd.flash.domain.RemainingActiveStock;

import java.util.UUID;

/**
 * Repository for RemainingActiveStock view.
 */
@Repository
public interface RemainingActiveStockRepository extends JpaRepository<RemainingActiveStock, UUID> {
}
