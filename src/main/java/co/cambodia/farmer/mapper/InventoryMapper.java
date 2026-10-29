package co.cambodia.farmer.mapper;

import co.cambodia.farmer.domain.InventoryMovement;
import co.cambodia.farmer.dto.inventory.InventoryMovementResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InventoryMapper {

    public InventoryMovementResponse toResponse(InventoryMovement movement) {
        if (movement == null) return null;

        UUID productId = movement.getProduct() != null ? movement.getProduct().getId() : null;
        String productName = movement.getProduct() != null ? movement.getProduct().getName() : null;
        String productNameKh = movement.getProduct() != null ? movement.getProduct().getNameKh() : null;
        String unit = movement.getProduct() != null ? movement.getProduct().getUnit() : "KG";

        UUID farmerId = movement.getFarmer() != null ? movement.getFarmer().getId() : null;
        String farmerName = movement.getFarmer() != null ? movement.getFarmer().getFullName() : null;

        return new InventoryMovementResponse(
                movement.getId(),
                productId,
                productName,
                productNameKh,
                unit,
                farmerId,
                farmerName,
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getPreviousStock(),
                movement.getNewStock(),
                movement.getCostPerUnit(),
                movement.getReferenceType(),
                movement.getReferenceId(),
                movement.getNote(),
                movement.getCreatedBy(),
                movement.getCreatedAt()
        );
    }
}
