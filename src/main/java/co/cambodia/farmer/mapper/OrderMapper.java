package co.cambodia.farmer.mapper;

import co.cambodia.farmer.domain.Order;
import co.cambodia.farmer.domain.OrderItem;
import co.cambodia.farmer.dto.order.OrderResponse;
import co.cambodia.farmer.dto.order.OrderItemResponse;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        if (order == null) return null;

        UUID customerId = (order.getCustomer() != null) ? order.getCustomer().getId() : null;
        String customerUsername = (order.getCustomer() != null) ? order.getCustomer().getUsername() : null;

        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getOrderItems() != null) {
            itemResponses = order.getOrderItems().stream()
                    .map(this::toItemResponse)
                    .collect(Collectors.toList());
        }

        return new OrderResponse(
                order.getId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getOrderDate(),
                customerId,
                customerUsername,
                itemResponses
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        if (item == null) return null;

        UUID productId = (item.getProduct() != null) ? item.getProduct().getId() : null;
        String productName = (item.getProduct() != null) ? item.getProduct().getName() : null;

        return new OrderItemResponse(
                item.getId(),
                productId,
                productName,
                item.getQuantity(),
                item.getPrice()
        );
    }
}
