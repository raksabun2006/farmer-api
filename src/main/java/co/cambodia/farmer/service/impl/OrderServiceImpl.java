package co.cambodia.farmer.service.impl;

import co.cambodia.farmer.domain.*;
import co.cambodia.farmer.dto.order.OrderItemRequest;
import co.cambodia.farmer.dto.order.OrderRequest;
import co.cambodia.farmer.dto.order.OrderResponse;
import co.cambodia.farmer.exception.ResourceNotFoundException;
import co.cambodia.farmer.mapper.OrderMapper;
import co.cambodia.farmer.repository.OrderRepository;
import co.cambodia.farmer.repository.ProductRepository;
import co.cambodia.farmer.repository.UserRepository;
import co.cambodia.farmer.service.InventoryService;
import co.cambodia.farmer.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        User customer = userRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("User account not found with id: " + request.customerId()));

        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setStatus("PENDING");
        order.setOrderItems(new ArrayList<>());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> itemsToSave = new ArrayList<>();

        for (OrderItemRequest itemReq : request.items()) {
            // Pessimistic lock guarantees concurrent orders cannot oversell the same product
            Product product = productRepository.findByIdWithLock(itemReq.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant missing with id: " + itemReq.productId()));

            if (product.getQuantity() < itemReq.quantity()) {
                throw new IllegalArgumentException(
                        String.format("Insufficient inventory for product '%s'. In stock: %d, Requested: %d",
                                product.getName(), product.getQuantity(), itemReq.quantity())
                );
            }

            // Deduct stock balance and record audit trail via InventoryService
            inventoryService.recordMovement(
                    product,
                    MovementType.STOCK_OUT_SALE,
                    itemReq.quantity(),
                    null,
                    MovementReferenceType.ORDER_SALE,
                    null, // Reference ID updated below after order is saved
                    "Order checkout purchase",
                    customer.getUsername()
            );

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemReq.quantity());
            orderItem.setPrice(product.getPrice());

            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));
            itemsToSave.add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        order.setOrderItems(itemsToSave);
        Order savedOrder = orderRepository.save(order);

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order invoice missing with id: " + id));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(UUID customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order missing with id: " + id));
        order.setStatus(status.toUpperCase());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public void cancelOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order missing with id: " + id));

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Order statement is already cancelled");
        }

        for (OrderItem item : order.getOrderItems()) {
            Product product = productRepository.findByIdWithLock(item.getProduct().getId())
                    .orElse(item.getProduct());

            inventoryService.recordMovement(
                    product,
                    MovementType.RETURN,
                    item.getQuantity(),
                    null,
                    MovementReferenceType.ORDER_CANCELLED,
                    order.getId(),
                    "Order #" + order.getId() + " cancellation return",
                    "system"
            );
        }

        order.setStatus("CANCELLED");
        orderRepository.save(order);
    }
}
