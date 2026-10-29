package co.cambodia.farmer.mapper;

import co.cambodia.farmer.domain.FarmTransaction;
import co.cambodia.farmer.dto.finance.FarmTransactionResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FinancialTransactionMapper {

    public FarmTransactionResponse toResponse(FarmTransaction transaction) {
        if (transaction == null) return null;

        UUID farmerId = transaction.getFarmer() != null ? transaction.getFarmer().getId() : null;
        String farmerName = transaction.getFarmer() != null ? transaction.getFarmer().getFullName() : null;

        return new FarmTransactionResponse(
                transaction.getId(),
                farmerId,
                farmerName,
                transaction.getType(),
                transaction.getCategory(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getExchangeRateToUsd(),
                transaction.getAmountInUsd(),
                transaction.getTransactionDate(),
                transaction.getPaymentMethod(),
                transaction.getReferenceType(),
                transaction.getReferenceId(),
                transaction.getDescription(),
                transaction.getDescriptionKh(),
                transaction.getReceiptImage(),
                transaction.getCreatedBy(),
                transaction.getCreatedAt()
        );
    }
}
