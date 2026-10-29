package co.cambodia.farmer.service.impl;

import co.cambodia.farmer.domain.*;
import co.cambodia.farmer.dto.finance.FarmTransactionRequest;
import co.cambodia.farmer.dto.finance.FarmTransactionResponse;
import co.cambodia.farmer.dto.finance.FinancialReportResponse;
import co.cambodia.farmer.exception.ResourceNotFoundException;
import co.cambodia.farmer.mapper.FinancialTransactionMapper;
import co.cambodia.farmer.repository.FarmTransactionRepository;
import co.cambodia.farmer.repository.FarmerRepository;
import co.cambodia.farmer.service.FinancialService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinancialServiceImpl implements FinancialService {

    private final FarmTransactionRepository transactionRepository;
    private final FarmerRepository farmerRepository;
    private final FinancialTransactionMapper transactionMapper;

    @Override
    @Transactional
    public FarmTransactionResponse recordTransaction(UUID farmerId, FarmTransactionRequest request, String actorUsername) {
        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found with id: " + farmerId));

        String currency = request.currency() != null && !request.currency().isBlank()
                ? request.currency().toUpperCase()
                : "USD";

        BigDecimal exchangeRate = request.exchangeRateToUsd();
        if ("KHR".equals(currency)) {
            if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                exchangeRate = BigDecimal.valueOf(4100.0);
            }
        } else {
            exchangeRate = BigDecimal.ONE;
        }

        BigDecimal amountInUsd = "KHR".equals(currency)
                ? request.amount().divide(exchangeRate, 2, RoundingMode.HALF_UP)
                : request.amount();

        FarmTransaction transaction = FarmTransaction.builder()
                .farmer(farmer)
                .type(request.type())
                .category(request.category())
                .amount(request.amount())
                .currency(currency)
                .exchangeRateToUsd(exchangeRate)
                .amountInUsd(amountInUsd)
                .transactionDate(request.transactionDate() != null ? request.transactionDate() : LocalDate.now())
                .paymentMethod(request.paymentMethod() != null ? request.paymentMethod() : PaymentMethod.CASH)
                .referenceType(request.referenceType())
                .referenceId(request.referenceId())
                .description(request.description())
                .descriptionKh(request.descriptionKh())
                .receiptImage(request.receiptImage())
                .createdBy(actorUsername != null ? actorUsername : "system")
                .createdAt(LocalDateTime.now())
                .build();

        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    @Override
    @Transactional(readOnly = true)
    public FarmTransactionResponse getTransactionById(UUID transactionId, UUID farmerId) {
        FarmTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial transaction not found with id: " + transactionId));

        if (farmerId != null && !transaction.getFarmer().getId().equals(farmerId)) {
            throw new IllegalArgumentException("Unauthorized: Transaction does not belong to this farmer");
        }

        return transactionMapper.toResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FarmTransactionResponse> getTransactionsByFarmer(UUID farmerId, TransactionType type, Pageable pageable) {
        if (!farmerRepository.existsById(farmerId)) {
            throw new ResourceNotFoundException("Farmer not found with id: " + farmerId);
        }

        if (type != null) {
            return transactionRepository.findByFarmerIdAndTypeOrderByTransactionDateDesc(farmerId, type, pageable)
                    .map(transactionMapper::toResponse);
        }

        return transactionRepository.findByFarmerIdOrderByTransactionDateDesc(farmerId, pageable)
                .map(transactionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialReportResponse getFinancialReport(UUID farmerId, LocalDate startDate, LocalDate endDate) {
        if (!farmerRepository.existsById(farmerId)) {
            throw new ResourceNotFoundException("Farmer not found with id: " + farmerId);
        }

        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        BigDecimal collectedCash = transactionRepository.sumAmountInUsdByFarmerIdAndTypeAndDateRange(
                farmerId, TransactionType.INCOME, start, end
        );
        if (collectedCash == null) collectedCash = BigDecimal.ZERO;

        BigDecimal totalExpenses = transactionRepository.sumAmountInUsdByFarmerIdAndTypeAndDateRange(
                farmerId, TransactionType.EXPENSE, start, end
        );
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

        Map<String, BigDecimal> expenseBreakdown = new HashMap<>();
        List<Object[]> expenseRows = transactionRepository.sumAmountByCategoryAndDateRange(
                farmerId, TransactionType.EXPENSE, start, end
        );
        for (Object[] row : expenseRows) {
            expenseBreakdown.put(row[0].toString(), (BigDecimal) row[1]);
        }

        Map<String, BigDecimal> incomeBreakdown = new HashMap<>();
        List<Object[]> incomeRows = transactionRepository.sumAmountByCategoryAndDateRange(
                farmerId, TransactionType.INCOME, start, end
        );
        for (Object[] row : incomeRows) {
            incomeBreakdown.put(row[0].toString(), (BigDecimal) row[1]);
        }

        BigDecimal netProfit = collectedCash.subtract(totalExpenses);

        return new FinancialReportResponse(
                farmerId,
                start,
                end,
                collectedCash, // Booked revenue
                collectedCash, // Collected cash
                totalExpenses, // Operating expenses
                BigDecimal.ZERO, // Outstanding receivables
                netProfit,
                expenseBreakdown,
                incomeBreakdown
        );
    }

    @Override
    @Transactional
    public void recordOrderPaymentIncome(Order order, Payment payment) {
        if (order == null || payment == null) return;

        // Group order items by farmer so each farmer gets credited their exact portion
        Map<Farmer, BigDecimal> farmerRevenue = new HashMap<>();
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getProduct() != null && item.getProduct().getFarmer() != null) {
                    Farmer farmer = item.getProduct().getFarmer();
                    BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    farmerRevenue.put(farmer, farmerRevenue.getOrDefault(farmer, BigDecimal.ZERO).add(itemTotal));
                }
            }
        }

        for (Map.Entry<Farmer, BigDecimal> entry : farmerRevenue.entrySet()) {
            Farmer farmer = entry.getKey();
            BigDecimal amount = entry.getValue();

            FarmTransaction tx = FarmTransaction.builder()
                    .farmer(farmer)
                    .type(TransactionType.INCOME)
                    .category(TransactionCategory.SALES_REVENUE)
                    .amount(amount)
                    .currency(payment.getCurrency() != null ? payment.getCurrency() : "USD")
                    .amountInUsd(amount)
                    .exchangeRateToUsd(BigDecimal.ONE)
                    .transactionDate(LocalDate.now())
                    .paymentMethod(PaymentMethod.BAKONG_KHQR)
                    .referenceType("ORDER")
                    .referenceId(order.getId())
                    .description("Bakong KHQR payment received for Order #" + order.getId())
                    .descriptionKh("ការទូទាត់បាគង KHQR បានទទួលសម្រាប់បញ្ជាទិញលេខ #" + order.getId())
                    .createdBy("system_khqr")
                    .createdAt(LocalDateTime.now())
                    .build();

            transactionRepository.save(tx);
        }
    }
}
