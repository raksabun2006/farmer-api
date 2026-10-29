package co.cambodia.farmer.service;

import co.cambodia.farmer.domain.MovementReferenceType;
import co.cambodia.farmer.domain.MovementType;
import co.cambodia.farmer.domain.Product;
import co.cambodia.farmer.dto.inventory.InventoryMovementResponse;
import co.cambodia.farmer.dto.inventory.InventorySummaryResponse;
import co.cambodia.farmer.dto.inventory.StockAdjustmentRequest;
import co.cambodia.farmer.dto.product.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface InventoryService {

    InventoryMovementResponse adjustStock(UUID farmerId, StockAdjustmentRequest request, String actorUsername);

    InventoryMovementResponse recordMovement(
            Product product,
            MovementType movementType,
            int quantityChange,
            BigDecimal costPerUnit,
            MovementReferenceType referenceType,
            UUID referenceId,
            String note,
            String actorUsername
    );

    Page<InventoryMovementResponse> getInventoryMovementsByProduct(UUID productId, UUID farmerId, Pageable pageable);

    Page<InventoryMovementResponse> getInventoryMovementsByFarmer(UUID farmerId, Pageable pageable);

    List<ProductResponse> getLowStockProducts(UUID farmerId);

    InventorySummaryResponse getInventorySummary(UUID farmerId);
}
