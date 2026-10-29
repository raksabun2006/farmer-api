package co.cambodia.farmer.service;

import co.cambodia.farmer.domain.*;
import co.cambodia.farmer.dto.order.OrderItemRequest;
import co.cambodia.farmer.dto.order.OrderItemResponse;
import co.cambodia.farmer.dto.order.OrderRequest;
import co.cambodia.farmer.dto.order.OrderResponse;
import co.cambodia.farmer.mapper.OrderMapper;
import co.cambodia.farmer.repository.OrderRepository;
import co.cambodia.farmer.repository.ProductRepository;
import co.cambodia.farmer.repository.UserRepository;
import co.cambodia.farmer.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceInventoryTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testCustomer;
    private Product testProduct;
    private final UUID customerId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testCustomer = new User();
        testCustomer.setId(customerId);
        testCustomer.setUsername("kiri_buyer");
        testCustomer.setRole(Role.CUSTOMER);

        Farmer farmer = new Farmer();
        farmer.setId(UUID.randomUUID());
        farmer.setFullName("Channary Farm");

        testProduct = new Product();
        testProduct.setId(productId);
        testProduct.setName("Kampot Pepper (Black)");
        testProduct.setPrice(BigDecimal.valueOf(12.00));
        testProduct.setQuantity(20);
        testProduct.setFarmer(farmer);
    }

    @Test
    @DisplayName("Create Order: Checks stock with lock, logs STOCK_OUT_SALE movement, and calculates total")
    void shouldCreateOrderAndRecordInventoryMovement() {
        OrderRequest request = new OrderRequest(
                customerId,
                List.of(new OrderItemRequest(productId, 3))
        );

        when(userRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(orderId);
            return o;
        });

        OrderResponse expectedResponse = new OrderResponse(
                orderId,
                BigDecimal.valueOf(36.00),
                "PENDING",
                LocalDateTime.now(),
                customerId,
                "kiri_buyer",
                List.of(new OrderItemResponse(UUID.randomUUID(), productId, "Kampot Pepper (Black)", 3, BigDecimal.valueOf(12.00)))
        );
        when(orderMapper.toResponse(any(Order.class))).thenReturn(expectedResponse);

        OrderResponse result = orderService.createOrder(request);

        assertThat(result).isNotNull();
        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(36.00));

        // Verify inventory movement was logged
        verify(inventoryService).recordMovement(
                eq(testProduct),
                eq(MovementType.STOCK_OUT_SALE),
                eq(3),
                isNull(),
                eq(MovementReferenceType.ORDER_SALE),
                isNull(),
                eq("Order checkout purchase"),
                eq("kiri_buyer")
        );
    }

    @Test
    @DisplayName("Create Order: Throws exception when requested quantity exceeds available stock")
    void shouldFailWhenInsufficientInventory() {
        OrderRequest request = new OrderRequest(
                customerId,
                List.of(new OrderItemRequest(productId, 50)) // Only 20 in stock
        );

        when(userRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient inventory");

        verify(orderRepository, never()).save(any());
        verify(inventoryService, never()).recordMovement(any(), any(), anyInt(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Cancel Order: Restores stock and records RETURN movement")
    void shouldCancelOrderAndRestoreStock() {
        Order order = new Order();
        order.setId(orderId);
        order.setStatus("PENDING");
        order.setCustomer(testCustomer);

        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setOrder(order);
        item.setProduct(testProduct);
        item.setQuantity(5);
        item.setPrice(BigDecimal.valueOf(12.00));

        order.setOrderItems(new ArrayList<>(List.of(item)));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(testProduct));

        orderService.cancelOrder(orderId);

        assertThat(order.getStatus()).isEqualTo("CANCELLED");
        verify(inventoryService).recordMovement(
                eq(testProduct),
                eq(MovementType.RETURN),
                eq(5),
                isNull(),
                eq(MovementReferenceType.ORDER_CANCELLED),
                eq(orderId),
                contains("cancellation return"),
                eq("system")
        );
        verify(orderRepository).save(order);
    }
}
