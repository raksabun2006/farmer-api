package co.cambodia.farmer.controller;

import co.cambodia.farmer.dto.inventory.InventoryMovementResponse;
import co.cambodia.farmer.dto.inventory.InventorySummaryResponse;
import co.cambodia.farmer.dto.inventory.StockAdjustmentRequest;
import co.cambodia.farmer.dto.product.ProductResponse;
import co.cambodia.farmer.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/adjust")
    public ResponseEntity<InventoryMovementResponse> adjustStock(
            @RequestParam(required = false) UUID farmerId,
            @Valid @RequestBody StockAdjustmentRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "farmer_user";
        InventoryMovementResponse response = inventoryService.adjustStock(farmerId, request, actor);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/movements/product/{productId}")
    public ResponseEntity<Page<InventoryMovementResponse>> getMovementsByProduct(
            @PathVariable UUID productId,
            @RequestParam(required = false) UUID farmerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(inventoryService.getInventoryMovementsByProduct(productId, farmerId, pageable));
    }

    @GetMapping("/movements/farmer/{farmerId}")
    public ResponseEntity<Page<InventoryMovementResponse>> getMovementsByFarmer(
            @PathVariable UUID farmerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(inventoryService.getInventoryMovementsByFarmer(farmerId, pageable));
    }

    @GetMapping("/low-stock/farmer/{farmerId}")
    public ResponseEntity<List<ProductResponse>> getLowStockProducts(@PathVariable UUID farmerId) {
        return ResponseEntity.ok(inventoryService.getLowStockProducts(farmerId));
    }

    @GetMapping("/summary/farmer/{farmerId}")
    public ResponseEntity<InventorySummaryResponse> getInventorySummary(@PathVariable UUID farmerId) {
        return ResponseEntity.ok(inventoryService.getInventorySummary(farmerId));
    }
}
