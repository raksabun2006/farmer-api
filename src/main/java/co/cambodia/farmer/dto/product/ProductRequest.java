package co.cambodia.farmer.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotBlank(message = "Product name is required")
        String name,

        String nameKh,

        String description,

        String descriptionKh,

        @NotNull(message = "Price is required")
        @Min(value = 0, message = "Price must be greater than or equal to 0")
        BigDecimal price,

        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative")
        Integer quantity,

        String unit,

        String status,

        Integer lowStockThreshold,

        String image,

        @NotNull(message = "Farmer ID association is required")
        UUID farmerId,

        @NotNull(message = "Category ID association is required")
        UUID categoryId
) {
    public ProductRequest(
            String name,
            String description,
            BigDecimal price,
            Integer quantity,
            String image,
            UUID farmerId,
            UUID categoryId
    ) {
        this(name, null, description, null, price, quantity, "KG", "ACTIVE", 10, image, farmerId, categoryId);
    }
}
