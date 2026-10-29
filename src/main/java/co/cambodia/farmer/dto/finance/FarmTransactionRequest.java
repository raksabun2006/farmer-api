package co.cambodia.farmer.dto.finance;

import co.cambodia.farmer.domain.PaymentMethod;
import co.cambodia.farmer.domain.TransactionCategory;
import co.cambodia.farmer.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FarmTransactionRequest(
        @NotNull(message = "Transaction type is required (INCOME or EXPENSE)")
        TransactionType type,

        @NotNull(message = "Category is required")
        TransactionCategory category,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        String currency,

        BigDecimal exchangeRateToUsd,

        LocalDate transactionDate,

        PaymentMethod paymentMethod,

        String referenceType,

        UUID referenceId,

        String description,

        String descriptionKh,

        String receiptImage
) {}
