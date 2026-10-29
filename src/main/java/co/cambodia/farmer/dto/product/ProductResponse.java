package co.cambodia.farmer.dto.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String nameKh,
        String description,
        String descriptionKh,
        BigDecimal price,
        Integer quantity,
        String unit,
        String status,
        Integer lowStockThreshold,
        boolean isLowStock,
        String image,
        UUID farmerId,
        String farmerName,
        String categoryName
) {
    public ProductResponse(
            UUID id,
            String name,
            String description,
            BigDecimal price,
            Integer quantity,
            String image,
            UUID farmerId,
            String farmerName,
            String categoryName
    ) {
        this(
                id,
                name,
                null,
                description,
                null,
                price,
                quantity,
                "KG",
                "ACTIVE",
                10,
                quantity != null && quantity <= 10,
                image,
                farmerId,
                farmerName,
                categoryName
        );
    }
}
