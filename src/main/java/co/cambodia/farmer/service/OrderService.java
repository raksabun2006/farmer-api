package co.cambodia.farmer.service;

import co.cambodia.farmer.dto.order.OrderRequest;
import co.cambodia.farmer.dto.order.OrderResponse;
import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);
    OrderResponse getOrderById(UUID id);
    List<OrderResponse> getAllOrders();
    List<OrderResponse> getOrdersByCustomerId(UUID customerId);
    OrderResponse updateOrderStatus(UUID id, String status);
    void cancelOrder(UUID id);
}
