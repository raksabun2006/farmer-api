package co.cambodia.farmer.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record FinancialReportResponse(
        UUID farmerId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalSalesRevenueUsd,
        BigDecimal totalCollectedCashUsd,
        BigDecimal totalExpensesUsd,
        BigDecimal outstandingReceivablesUsd,
        BigDecimal estimatedNetProfitUsd,
        Map<String, BigDecimal> expenseBreakdownByCategory,
        Map<String, BigDecimal> incomeBreakdownByCategory
) {}
