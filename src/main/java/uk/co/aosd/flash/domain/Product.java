package uk.co.aosd.flash.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product Entity.
 */
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(name = "total_physical_stock", nullable = false)
    private Integer totalPhysicalStock;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    @Column(name = "reserved_count", nullable = false)
    private Integer reservedCount;
}
