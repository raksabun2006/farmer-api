package co.cambodia.farmer.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        BigDecimal totalAmount,
        String status,
        LocalDateTime orderDate,
        Long customerId,
        String customerUsername,
        List<OrderItemResponse> items
) {}