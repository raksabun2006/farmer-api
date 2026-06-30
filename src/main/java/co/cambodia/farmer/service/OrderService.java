package co.cambodia.farmer.service;

import co.cambodia.farmer.dto.order.OrderRequest;
import co.cambodia.farmer.dto.order.OrderResponse;
import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);
    OrderResponse getOrderById(Long id);
    List<OrderResponse> getAllOrders();
    List<OrderResponse> getOrdersByCustomerId(Long customerId);
    OrderResponse updateOrderStatus(Long id, String status);
    void cancelOrder(Long id);


}