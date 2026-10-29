package co.cambodia.farmer.service;

import co.cambodia.farmer.domain.Order;
import co.cambodia.farmer.domain.Payment;
import co.cambodia.farmer.domain.TransactionType;
import co.cambodia.farmer.dto.finance.FarmTransactionRequest;
import co.cambodia.farmer.dto.finance.FarmTransactionResponse;
import co.cambodia.farmer.dto.finance.FinancialReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface FinancialService {

    FarmTransactionResponse recordTransaction(UUID farmerId, FarmTransactionRequest request, String actorUsername);

    FarmTransactionResponse getTransactionById(UUID transactionId, UUID farmerId);

    Page<FarmTransactionResponse> getTransactionsByFarmer(UUID farmerId, TransactionType type, Pageable pageable);

    FinancialReportResponse getFinancialReport(UUID farmerId, LocalDate startDate, LocalDate endDate);

    void recordOrderPaymentIncome(Order order, Payment payment);
}
