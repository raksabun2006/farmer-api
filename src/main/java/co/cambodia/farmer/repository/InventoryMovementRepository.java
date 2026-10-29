package co.cambodia.farmer.repository;

import co.cambodia.farmer.domain.InventoryMovement;
import co.cambodia.farmer.domain.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

    Page<InventoryMovement> findByFarmerIdOrderByCreatedAtDesc(UUID farmerId, Pageable pageable);

    Page<InventoryMovement> findByProductIdAndFarmerIdOrderByCreatedAtDesc(UUID productId, UUID farmerId, Pageable pageable);

    Page<InventoryMovement> findByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);

    Page<InventoryMovement> findByFarmerIdAndMovementTypeOrderByCreatedAtDesc(
            UUID farmerId, MovementType movementType, Pageable pageable
    );

    Page<InventoryMovement> findByFarmerIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID farmerId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(m.quantity), 0) FROM InventoryMovement m " +
           "WHERE m.farmer.id = :farmerId AND m.movementType = :movementType")
    Long sumQuantityByFarmerIdAndMovementType(
            @Param("farmerId") UUID farmerId,
            @Param("movementType") MovementType movementType
    );
}
