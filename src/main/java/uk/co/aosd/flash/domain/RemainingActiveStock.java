package uk.co.aosd.flash.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * JPA entity for the RemainingActiveStock view.
 */
@Entity
@Table(name = "remaining_active_stock")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemainingActiveStock {
    @Id
    @Column(name = "item_id")
    private UUID itemId;

    @Column(name = "id")
    private UUID saleId;

    @Column(name = "title")
    private String title;

    @Column(name = "start_time")
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "allocated_stock")
    private Integer allocatedStock;

    @Column(name = "sold_count")
    private Integer soldCount;

    @Column(name = "sale_price")
    private BigDecimal salePrice;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_description")
    private String productDescription;

    @Column(name = "base_price")
    private BigDecimal basePrice;
}
