package co.cambodia.farmer.dto.inventory;

import java.util.UUID;

public record InventorySummaryResponse(
        UUID farmerId,
        long totalProducts,
        long activeProducts,
        long lowStockCount,
        long outOfStockCount,
        long totalStockInUnits,
        long totalStockOutUnits
) {}
