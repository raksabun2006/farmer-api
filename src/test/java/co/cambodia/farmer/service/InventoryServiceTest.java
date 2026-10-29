package co.cambodia.farmer.service;

import co.cambodia.farmer.domain.*;
import co.cambodia.farmer.dto.inventory.InventoryMovementResponse;
import co.cambodia.farmer.dto.inventory.StockAdjustmentRequest;
import co.cambodia.farmer.dto.product.ProductResponse;
import co.cambodia.farmer.mapper.InventoryMapper;
import co.cambodia.farmer.mapper.ProductMapper;
import co.cambodia.farmer.repository.FarmerRepository;
import co.cambodia.farmer.repository.InventoryMovementRepository;
import co.cambodia.farmer.repository.ProductRepository;
import co.cambodia.farmer.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private InventoryMovementRepository movementRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Farmer testFarmer;
    private Product testProduct;
    private final UUID farmerId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testFarmer = new Farmer();
        testFarmer.setId(farmerId);
        testFarmer.setFullName("Sokha Meas");

        testProduct = new Product();
        testProduct.setId(productId);
        testProduct.setName("Battambang Jasmine Rice");
        testProduct.setNameKh("ស្រូវម្លិះបាត់ដំបង");
        testProduct.setPrice(BigDecimal.valueOf(1.50));
        testProduct.setQuantity(50);
        testProduct.setUnit("KG");
        testProduct.setStatus("ACTIVE");
        testProduct.setLowStockThreshold(10);
        testProduct.setFarmer(testFarmer);
    }

    @Test
    @DisplayName("Stock In: Adds quantity and persists inventory movement")
    void shouldAddStockSuccessfully() {
        StockAdjustmentRequest request = new StockAdjustmentRequest(
                productId,
                MovementType.STOCK_IN,
                25,
                BigDecimal.valueOf(1.10),
                MovementReferenceType.PURCHASE_RECEIPT,
                null,
                "Restocked from harvest"
        );

        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(testProduct));
        when(movementRepository.save(any(InventoryMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryMapper.toResponse(any(InventoryMovement.class))).thenAnswer(inv -> {
            InventoryMovement m = inv.getArgument(0);
            return new InventoryMovementResponse(
                    UUID.randomUUID(), productId, "Battambang Jasmine Rice", "ស្រូវម្លិះបាត់ដំបង", "KG",
                    farmerId, "Sokha Meas", m.getMovementType(), m.getQuantity(),
                    m.getPreviousStock(), m.getNewStock(), m.getCostPerUnit(),
                    m.getReferenceType(), m.getReferenceId(), m.getNote(),
                    m.getCreatedBy(), null
            );
        });

        InventoryMovementResponse response = inventoryService.adjustStock(farmerId, request, "farmer_user");

        assertThat(testProduct.getQuantity()).isEqualTo(75);
        assertThat(testProduct.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.previousStock()).isEqualTo(50);
        assertThat(response.newStock()).isEqualTo(75);
        assertThat(response.quantity()).isEqualTo(25);

        ArgumentCaptor<InventoryMovement> movementCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(movementCaptor.capture());
        InventoryMovement savedMovement = movementCaptor.getValue();
        assertThat(savedMovement.getMovementType()).isEqualTo(MovementType.STOCK_IN);
        assertThat(savedMovement.getNewStock()).isEqualTo(75);
    }

    @Test
    @DisplayName("Stock Out Waste: Deducts quantity and sets OUT_OF_STOCK when zero")
    void shouldDeductStockAndMarkOutOfStockWhenReachingZero() {
        StockAdjustmentRequest request = new StockAdjustmentRequest(
                productId,
                MovementType.STOCK_OUT_WASTE,
                50,
                null,
                MovementReferenceType.MANUAL_ADJUSTMENT,
                null,
                "Damaged by monsoon flood"
        );

        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(testProduct));
        when(movementRepository.save(any(InventoryMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.adjustStock(farmerId, request, "farmer_user");

        assertThat(testProduct.getQuantity()).isEqualTo(0);
        assertThat(testProduct.getStatus()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    @DisplayName("Stock Out: Throws exception when requested reduction exceeds available stock")
    void shouldThrowWhenStockIsInsufficient() {
        StockAdjustmentRequest request = new StockAdjustmentRequest(
                productId,
                MovementType.STOCK_OUT_WASTE,
                100, // Available is only 50
                null,
                MovementReferenceType.MANUAL_ADJUSTMENT,
                null,
                "Trying to deduct more than stock"
        );

        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> inventoryService.adjustStock(farmerId, request, "farmer_user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");

        verify(movementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Security: Throws exception when farmer tries to adjust another farmer's product")
    void shouldRejectAdjustmentWhenFarmerDoesNotOwnProduct() {
        StockAdjustmentRequest request = new StockAdjustmentRequest(
                productId,
                MovementType.STOCK_IN,
                10,
                null,
                MovementReferenceType.MANUAL_ADJUSTMENT,
                null,
                "Unauthorized test"
        );

        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(testProduct));

        UUID otherFarmerId = UUID.randomUUID();
        assertThatThrownBy(() -> inventoryService.adjustStock(otherFarmerId, request, "other_user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unauthorized: Product does not belong to this farmer");
    }

    @Test
    @DisplayName("Reconciliation Adjustment: Overwrites stock to exact physical count")
    void shouldHandleReconciliationAdjustment() {
        StockAdjustmentRequest request = new StockAdjustmentRequest(
                productId,
                MovementType.ADJUSTMENT,
                42, // Physical inventory count found 42 kg
                null,
                MovementReferenceType.MANUAL_ADJUSTMENT,
                null,
                "Monthly warehouse count audit"
        );

        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(testProduct));
        when(movementRepository.save(any(InventoryMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.adjustStock(farmerId, request, "auditor");

        assertThat(testProduct.getQuantity()).isEqualTo(42);
        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(captor.capture());
        assertThat(captor.getValue().getMovementType()).isEqualTo(MovementType.ADJUSTMENT);
        assertThat(captor.getValue().getPreviousStock()).isEqualTo(50);
        assertThat(captor.getValue().getNewStock()).isEqualTo(42);
        assertThat(captor.getValue().getQuantity()).isEqualTo(8); // Delta is 8
    }

    @Test
    @DisplayName("Low Stock: Retrieves products below low-stock threshold")
    void shouldReturnLowStockProducts() {
        when(farmerRepository.existsById(farmerId)).thenReturn(true);
        testProduct.setQuantity(5); // Below threshold 10
        when(productRepository.findLowStockProductsByFarmerId(farmerId)).thenReturn(List.of(testProduct));
        when(productMapper.toResponse(testProduct)).thenReturn(
                new ProductResponse(productId, "Battambang Jasmine Rice", null, null, null,
                        BigDecimal.valueOf(1.5), 5, "KG", "ACTIVE", 10, true, null, farmerId, "Sokha", "Rice")
        );

        List<ProductResponse> lowStock = inventoryService.getLowStockProducts(farmerId);
        assertThat(lowStock).hasSize(1);
        assertThat(lowStock.get(0).isLowStock()).isTrue();
        assertThat(lowStock.get(0).quantity()).isEqualTo(5);
    }
}
