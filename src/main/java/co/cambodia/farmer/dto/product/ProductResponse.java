package co.cambodia.farmer.dto.product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer quantity,
        String image,
        Long farmerId,
        String farmerName,
        String categoryName
) {}