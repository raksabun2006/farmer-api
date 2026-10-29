package co.cambodia.farmer.controller;

import co.cambodia.farmer.domain.TransactionType;
import co.cambodia.farmer.dto.finance.FarmTransactionRequest;
import co.cambodia.farmer.dto.finance.FarmTransactionResponse;
import co.cambodia.farmer.dto.finance.FinancialReportResponse;
import co.cambodia.farmer.service.FinancialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinancialController {

    private final FinancialService financialService;

    @PostMapping("/transactions")
    public ResponseEntity<FarmTransactionResponse> createTransaction(
            @RequestParam UUID farmerId,
            @Valid @RequestBody FarmTransactionRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "farmer_user";
        FarmTransactionResponse response = financialService.recordTransaction(farmerId, request, actor);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<FarmTransactionResponse> getTransactionById(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID farmerId
    ) {
        return ResponseEntity.ok(financialService.getTransactionById(id, farmerId));
    }

    @GetMapping("/transactions/farmer/{farmerId}")
    public ResponseEntity<Page<FarmTransactionResponse>> getTransactionsByFarmer(
            @PathVariable UUID farmerId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        return ResponseEntity.ok(financialService.getTransactionsByFarmer(farmerId, type, pageable));
    }

    @GetMapping("/reports/farmer/{farmerId}")
    public ResponseEntity<FinancialReportResponse> getFinancialReport(
            @PathVariable UUID farmerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(financialService.getFinancialReport(farmerId, startDate, endDate));
    }
}
