package co.cambodia.farmer.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_kh")
    private String nameKh;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_kh", columnDefinition = "TEXT")
    private String descriptionKh;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 50, nullable = false)
    private String unit = "KG"; // e.g., KG, G, TON, BUNCH, CRATE, SACK, PIECE

    @Column(length = 30, nullable = false)
    private String status = "ACTIVE"; // ACTIVE, OUT_OF_STOCK, INACTIVE, ARCHIVED

    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold = 10;

    @Column(columnDefinition = "TEXT")
    private String image;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private Farmer farmer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<OrderItem> orderItems;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
        if (this.unit == null || this.unit.isBlank()) {
            this.unit = "KG";
        }
        if (this.status == null || this.status.isBlank()) {
            this.status = "ACTIVE";
        }
        if (this.lowStockThreshold == null) {
            this.lowStockThreshold = 10;
        }
        if (this.version == null) {
            this.version = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.quantity != null && this.quantity <= 0 && "ACTIVE".equals(this.status)) {
            this.status = "OUT_OF_STOCK";
        } else if (this.quantity != null && this.quantity > 0 && "OUT_OF_STOCK".equals(this.status)) {
            this.status = "ACTIVE";
        }
    }
}
