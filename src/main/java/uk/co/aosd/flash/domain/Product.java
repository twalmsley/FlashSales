package uk.co.aosd.flash.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

// --- Product Entity ---
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "total_physical_stock", nullable = false)
    private Integer totalPhysicalStock;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;
}
