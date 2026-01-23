package uk.co.aosd.flash.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uk.co.aosd.flash.domain.Order;

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
}
