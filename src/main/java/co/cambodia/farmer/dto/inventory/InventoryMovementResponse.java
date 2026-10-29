package co.cambodia.farmer.dto.inventory;

import co.cambodia.farmer.domain.MovementReferenceType;
import co.cambodia.farmer.domain.MovementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryMovementResponse(
        UUID id,
        UUID productId,
        String productName,
        String productNameKh,
        String unit,
        UUID farmerId,
        String farmerName,
        MovementType movementType,
        Integer quantity,
        Integer previousStock,
        Integer newStock,
        BigDecimal costPerUnit,
        MovementReferenceType referenceType,
        UUID referenceId,
        String note,
        String createdBy,
        LocalDateTime createdAt
) {}
