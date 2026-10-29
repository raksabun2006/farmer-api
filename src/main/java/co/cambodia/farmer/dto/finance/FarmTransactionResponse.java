package co.cambodia.farmer.dto.finance;

import co.cambodia.farmer.domain.PaymentMethod;
import co.cambodia.farmer.domain.TransactionCategory;
import co.cambodia.farmer.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record FarmTransactionResponse(
        UUID id,
        UUID farmerId,
        String farmerName,
        TransactionType type,
        TransactionCategory category,
        BigDecimal amount,
        String currency,
        BigDecimal exchangeRateToUsd,
        BigDecimal amountInUsd,
        LocalDate transactionDate,
        PaymentMethod paymentMethod,
        String referenceType,
        UUID referenceId,
        String description,
        String descriptionKh,
        String receiptImage,
        String createdBy,
        LocalDateTime createdAt
) {}
