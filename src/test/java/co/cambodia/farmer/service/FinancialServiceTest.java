package co.cambodia.farmer.service;

import co.cambodia.farmer.domain.*;
import co.cambodia.farmer.dto.finance.FarmTransactionRequest;
import co.cambodia.farmer.dto.finance.FarmTransactionResponse;
import co.cambodia.farmer.dto.finance.FinancialReportResponse;
import co.cambodia.farmer.mapper.FinancialTransactionMapper;
import co.cambodia.farmer.repository.FarmTransactionRepository;
import co.cambodia.farmer.repository.FarmerRepository;
import co.cambodia.farmer.service.impl.FinancialServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialServiceTest {

    @Mock
    private FarmTransactionRepository transactionRepository;

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private FinancialTransactionMapper transactionMapper;

    @InjectMocks
    private FinancialServiceImpl financialService;

    private Farmer testFarmer;
    private final UUID farmerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testFarmer = new Farmer();
        testFarmer.setId(farmerId);
        testFarmer.setFullName("Chhim Vannak");
    }

    @Test
    @DisplayName("Record Expense (USD): Saves expense transaction accurately")
    void shouldRecordUsdExpense() {
        FarmTransactionRequest request = new FarmTransactionRequest(
                TransactionType.EXPENSE,
                TransactionCategory.FERTILIZER,
                BigDecimal.valueOf(150.00),
                "USD",
                null,
                LocalDate.now(),
                PaymentMethod.CASH,
                "MANUAL",
                null,
                "Organic compost fertilizer",
                "ជីកំប៉ុស្តសរីរាង្គ",
                null
        );

        UUID txId = UUID.randomUUID();
        when(farmerRepository.findById(farmerId)).thenReturn(Optional.of(testFarmer));
        when(transactionRepository.save(any(FarmTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toResponse(any(FarmTransaction.class))).thenAnswer(inv -> {
            FarmTransaction tx = inv.getArgument(0);
            return new FarmTransactionResponse(
                    txId, farmerId, "Chhim Vannak", tx.getType(), tx.getCategory(),
                    tx.getAmount(), tx.getCurrency(), tx.getExchangeRateToUsd(),
                    tx.getAmountInUsd(), tx.getTransactionDate(), tx.getPaymentMethod(),
                    tx.getReferenceType(), tx.getReferenceId(), tx.getDescription(),
                    tx.getDescriptionKh(), tx.getReceiptImage(), tx.getCreatedBy(), null
            );
        });

        FarmTransactionResponse response = financialService.recordTransaction(farmerId, request, "vannak_user");

        assertThat(response).isNotNull();
        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
        assertThat(response.amountInUsd()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
        assertThat(response.category()).isEqualTo(TransactionCategory.FERTILIZER);
    }

    @Test
    @DisplayName("Record Expense (KHR): Converts KHR to USD accurately with default exchange rate")
    void shouldConvertKhrExpenseToUsd() {
        // 410,000 KHR / 4100 = 100.00 USD
        FarmTransactionRequest request = new FarmTransactionRequest(
                TransactionType.EXPENSE,
                TransactionCategory.FUEL,
                BigDecimal.valueOf(410000.00),
                "KHR",
                BigDecimal.valueOf(4100.0),
                LocalDate.now(),
                PaymentMethod.BAKONG_KHQR,
                "MANUAL",
                null,
                "Diesel for tractor",
                "ប្រេងម៉ាស៊ូតសម្រាប់គោយន្ត",
                null
        );

        when(farmerRepository.findById(farmerId)).thenReturn(Optional.of(testFarmer));
        when(transactionRepository.save(any(FarmTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        financialService.recordTransaction(farmerId, request, "vannak_user");

        ArgumentCaptor<FarmTransaction> captor = ArgumentCaptor.forClass(FarmTransaction.class);
        verify(transactionRepository).save(captor.capture());
        FarmTransaction saved = captor.getValue();
        assertThat(saved.getCurrency()).isEqualTo("KHR");
        assertThat(saved.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(410000.00));
        assertThat(saved.getAmountInUsd()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    @DisplayName("Financial Report: Accurately calculates cash, expenses, and net profit")
    void shouldCalculateAccurateFinancialReport() {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        when(farmerRepository.existsById(farmerId)).thenReturn(true);
        // Income = 2,500 USD, Expenses = 1,200 USD -> Profit = 1,300 USD
        when(transactionRepository.sumAmountInUsdByFarmerIdAndTypeAndDateRange(farmerId, TransactionType.INCOME, start, end))
                .thenReturn(BigDecimal.valueOf(2500.00));
        when(transactionRepository.sumAmountInUsdByFarmerIdAndTypeAndDateRange(farmerId, TransactionType.EXPENSE, start, end))
                .thenReturn(BigDecimal.valueOf(1200.00));

        List<Object[]> expenseRows = new ArrayList<>();
        expenseRows.add(new Object[]{TransactionCategory.SEEDS.name(), BigDecimal.valueOf(400.00)});
        expenseRows.add(new Object[]{TransactionCategory.LABOR.name(), BigDecimal.valueOf(800.00)});
        when(transactionRepository.sumAmountByCategoryAndDateRange(farmerId, TransactionType.EXPENSE, start, end))
                .thenReturn(expenseRows);

        List<Object[]> incomeRows = new ArrayList<>();
        incomeRows.add(new Object[]{TransactionCategory.SALES_REVENUE.name(), BigDecimal.valueOf(2500.00)});
        when(transactionRepository.sumAmountByCategoryAndDateRange(farmerId, TransactionType.INCOME, start, end))
                .thenReturn(incomeRows);

        FinancialReportResponse report = financialService.getFinancialReport(farmerId, start, end);

        assertThat(report.totalCollectedCashUsd()).isEqualByComparingTo(BigDecimal.valueOf(2500.00));
        assertThat(report.totalExpensesUsd()).isEqualByComparingTo(BigDecimal.valueOf(1200.00));
        assertThat(report.estimatedNetProfitUsd()).isEqualByComparingTo(BigDecimal.valueOf(1300.00));
        assertThat(report.expenseBreakdownByCategory()).containsEntry("SEEDS", BigDecimal.valueOf(400.00));
        assertThat(report.expenseBreakdownByCategory()).containsEntry("LABOR", BigDecimal.valueOf(800.00));
    }

    @Test
    @DisplayName("Security: Rejects access when requesting transaction belonging to another farmer")
    void shouldRejectAccessForUnownedTransaction() {
        UUID otherFarmerId = UUID.randomUUID();
        Farmer otherFarmer = new Farmer();
        otherFarmer.setId(otherFarmerId);

        UUID txId = UUID.randomUUID();
        FarmTransaction tx = new FarmTransaction();
        tx.setId(txId);
        tx.setFarmer(otherFarmer);

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> financialService.getTransactionById(txId, farmerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unauthorized: Transaction does not belong to this farmer");
    }
}
