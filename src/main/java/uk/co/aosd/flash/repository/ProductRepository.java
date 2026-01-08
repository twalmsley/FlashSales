package uk.co.aosd.flash.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import uk.co.aosd.flash.domain.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
}
