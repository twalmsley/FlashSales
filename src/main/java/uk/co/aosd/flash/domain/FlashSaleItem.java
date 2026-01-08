package uk.co.aosd.flash.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "flash_sale_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "flash_sale_id", "product_id" })
})
@Getter
@Setter
@NoArgsConstructor
public class FlashSaleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flash_sale_id", nullable = false)
    private FlashSale flashSale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "allocated_stock", nullable = false)
    private Integer allocatedStock;

    @Column(name = "sold_count", nullable = false)
    private Integer soldCount = 0;

    @Column(name = "sale_price", nullable = false)
    private BigDecimal salePrice;
}
