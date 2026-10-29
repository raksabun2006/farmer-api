package co.cambodia.farmer.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        BigDecimal totalAmount,
        String status,
        LocalDateTime orderDate,
        UUID customerId,
        String customerUsername,
        List<OrderItemResponse> items
) {}
