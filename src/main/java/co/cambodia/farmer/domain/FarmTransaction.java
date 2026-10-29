package co.cambodia.farmer.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "farm_transactions",
    indexes = {
        @Index(name = "idx_tx_farmer_date", columnList = "farmer_id, transaction_date DESC"),
        @Index(name = "idx_tx_farmer_type", columnList = "farmer_id, transaction_type"),
        @Index(name = "idx_tx_category", columnList = "farmer_id, category")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private Farmer farmer;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TransactionCategory category;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency; // "USD" or "KHR"

    @Column(name = "exchange_rate_to_usd", precision = 10, scale = 4)
    private BigDecimal exchangeRateToUsd; // e.g., 4100.0000 for KHR, 1.0000 for USD

    @Column(name = "amount_in_usd", nullable = false, precision = 14, scale = 2)
    private BigDecimal amountInUsd;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "reference_type", length = 50)
    private String referenceType; // e.g., "ORDER", "HARVEST", "MANUAL"

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_kh", columnDefinition = "TEXT")
    private String descriptionKh;

    @Column(name = "receipt_image", columnDefinition = "TEXT")
    private String receiptImage;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.currency == null || this.currency.isBlank()) {
            this.currency = "USD";
        }
        if (this.transactionDate == null) {
            this.transactionDate = LocalDate.now();
        }
        if ("KHR".equalsIgnoreCase(this.currency)) {
            if (this.exchangeRateToUsd == null || this.exchangeRateToUsd.compareTo(BigDecimal.ZERO) <= 0) {
                this.exchangeRateToUsd = BigDecimal.valueOf(4100.0);
            }
            this.amountInUsd = this.amount.divide(this.exchangeRateToUsd, 2, RoundingMode.HALF_UP);
        } else {
            this.exchangeRateToUsd = BigDecimal.ONE;
            this.amountInUsd = this.amount;
        }
    }
}
