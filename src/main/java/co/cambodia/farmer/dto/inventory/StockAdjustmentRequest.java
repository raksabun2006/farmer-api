package co.cambodia.farmer.dto.inventory;

import co.cambodia.farmer.domain.MovementReferenceType;
import co.cambodia.farmer.domain.MovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record StockAdjustmentRequest(
        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotNull(message = "Movement type is required")
        MovementType movementType,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @Min(value = 0, message = "Cost per unit cannot be negative")
        BigDecimal costPerUnit,

        MovementReferenceType referenceType,

        UUID referenceId,

        String note
) {}
