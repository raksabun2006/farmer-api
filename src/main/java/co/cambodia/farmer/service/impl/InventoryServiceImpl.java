package co.cambodia.farmer.service.impl;

import co.cambodia.farmer.domain.*;
import co.cambodia.farmer.dto.inventory.InventoryMovementResponse;
import co.cambodia.farmer.dto.inventory.InventorySummaryResponse;
import co.cambodia.farmer.dto.inventory.StockAdjustmentRequest;
import co.cambodia.farmer.dto.product.ProductResponse;
import co.cambodia.farmer.exception.ResourceNotFoundException;
import co.cambodia.farmer.mapper.InventoryMapper;
import co.cambodia.farmer.mapper.ProductMapper;
import co.cambodia.farmer.repository.FarmerRepository;
import co.cambodia.farmer.repository.InventoryMovementRepository;
import co.cambodia.farmer.repository.ProductRepository;
import co.cambodia.farmer.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final FarmerRepository farmerRepository;
    private final InventoryMovementRepository movementRepository;
    private final InventoryMapper inventoryMapper;
    private final ProductMapper productMapper;


    @Override
    @Transactional
    public InventoryMovementResponse adjustStock(UUID farmerId, StockAdjustmentRequest request, String actorUsername) {
        Product product = productRepository.findByIdWithLock(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        if (farmerId != null && !product.getFarmer().getId().equals(farmerId)) {
            throw new IllegalArgumentException("Unauthorized: Product does not belong to this farmer");
        }

        int previousStock = product.getQuantity();
        int newStock;
        int movementQuantity;

        switch (request.movementType()) {
            case STOCK_IN:
            case RETURN:
                movementQuantity = request.quantity();
                newStock = previousStock + movementQuantity;
                break;
            case STOCK_OUT_SALE:
            case STOCK_OUT_WASTE:
            case STOCK_OUT_DAMAGE:
                movementQuantity = request.quantity();
                if (previousStock < movementQuantity) {
                    throw new IllegalArgumentException(
                            String.format("Insufficient stock for '%s'. Available: %d, Requested reduction: %d",
                                    product.getName(), previousStock, movementQuantity)
                    );
                }
                newStock = previousStock - movementQuantity;
                break;
            case ADJUSTMENT:
                // For reconciliation adjustment, request.quantity represents the actual physical count
                newStock = request.quantity();
                movementQuantity = Math.abs(newStock - previousStock);
                break;
            default:
                throw new IllegalArgumentException("Unsupported movement type: " + request.movementType());
        }

        product.setQuantity(newStock);
        if (newStock <= 0) {
            product.setStatus("OUT_OF_STOCK");
        } else if ("OUT_OF_STOCK".equals(product.getStatus())) {
            product.setStatus("ACTIVE");
        }
        productRepository.save(product);

        MovementReferenceType refType = request.referenceType() != null
                ? request.referenceType()
                : MovementReferenceType.MANUAL_ADJUSTMENT;

        InventoryMovement movement = InventoryMovement.builder()
                .product(product)
                .farmer(product.getFarmer())
                .movementType(request.movementType())
                .quantity(movementQuantity)
                .previousStock(previousStock)
                .newStock(newStock)
                .costPerUnit(request.costPerUnit())
                .referenceType(refType)
                .referenceId(request.referenceId())
                .note(request.note())
                .createdBy(actorUsername != null ? actorUsername : "system")
                .build();

        return inventoryMapper.toResponse(movementRepository.save(movement));
    }

    @Override
    @Transactional
    public InventoryMovementResponse recordMovement(
            Product product,
            MovementType movementType,
            int quantityChange,
            BigDecimal costPerUnit,
            MovementReferenceType referenceType,
            UUID referenceId,
            String note,
            String actorUsername
    ) {
        int previousStock = product.getQuantity();
        int newStock;

        if (movementType == MovementType.STOCK_IN || movementType == MovementType.RETURN) {
            newStock = previousStock + quantityChange;
        } else {
            if (previousStock < quantityChange) {
                throw new IllegalArgumentException(
                        String.format("Insufficient stock for '%s'. Available: %d, Requested: %d",
                                product.getName(), previousStock, quantityChange)
                );
            }
            newStock = previousStock - quantityChange;
        }

        product.setQuantity(newStock);
        if (newStock <= 0) {
            product.setStatus("OUT_OF_STOCK");
        } else if ("OUT_OF_STOCK".equals(product.getStatus())) {
            product.setStatus("ACTIVE");
        }
        productRepository.save(product);

        InventoryMovement movement = InventoryMovement.builder()
                .product(product)
                .farmer(product.getFarmer())
                .movementType(movementType)
                .quantity(quantityChange)
                .previousStock(previousStock)
                .newStock(newStock)
                .costPerUnit(costPerUnit)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .note(note)
                .createdBy(actorUsername != null ? actorUsername : "system")
                .build();

        return inventoryMapper.toResponse(movementRepository.save(movement));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryMovementResponse> getInventoryMovementsByProduct(UUID productId, UUID farmerId, Pageable pageable) {
        if (farmerId != null) {
            return movementRepository.findByProductIdAndFarmerIdOrderByCreatedAtDesc(productId, farmerId, pageable)
                    .map(inventoryMapper::toResponse);
        }
        return movementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(inventoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryMovementResponse> getInventoryMovementsByFarmer(UUID farmerId, Pageable pageable) {
        if (!farmerRepository.existsById(farmerId)) {
            throw new ResourceNotFoundException("Farmer not found with id: " + farmerId);
        }
        return movementRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId, pageable)
                .map(inventoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts(UUID farmerId) {
        if (!farmerRepository.existsById(farmerId)) {
            throw new ResourceNotFoundException("Farmer not found with id: " + farmerId);
        }
        return productRepository.findLowStockProductsByFarmerId(farmerId).stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InventorySummaryResponse getInventorySummary(UUID farmerId) {
        if (!farmerRepository.existsById(farmerId)) {
            throw new ResourceNotFoundException("Farmer not found with id: " + farmerId);
        }

        List<Product> products = productRepository.findByFarmerId(farmerId);
        long totalProducts = products.size();
        long activeProducts = products.stream().filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus())).count();
        long lowStockCount = products.stream().filter(p -> p.getQuantity() != null && p.getQuantity() <= (p.getLowStockThreshold() != null ? p.getLowStockThreshold() : 10)).count();
        long outOfStockCount = products.stream().filter(p -> p.getQuantity() != null && p.getQuantity() <= 0).count();

        Long totalStockIn = movementRepository.sumQuantityByFarmerIdAndMovementType(farmerId, MovementType.STOCK_IN);
        Long totalStockOutSales = movementRepository.sumQuantityByFarmerIdAndMovementType(farmerId, MovementType.STOCK_OUT_SALE);
        Long totalStockOutWaste = movementRepository.sumQuantityByFarmerIdAndMovementType(farmerId, MovementType.STOCK_OUT_WASTE);
        Long totalStockOutDamage = movementRepository.sumQuantityByFarmerIdAndMovementType(farmerId, MovementType.STOCK_OUT_DAMAGE);

        long totalStockOut = (totalStockOutSales != null ? totalStockOutSales : 0L)
                + (totalStockOutWaste != null ? totalStockOutWaste : 0L)
                + (totalStockOutDamage != null ? totalStockOutDamage : 0L);

        return new InventorySummaryResponse(
                farmerId,
                totalProducts,
                activeProducts,
                lowStockCount,
                outOfStockCount,
                totalStockIn != null ? totalStockIn : 0L,
                totalStockOut
        );
    }
}
