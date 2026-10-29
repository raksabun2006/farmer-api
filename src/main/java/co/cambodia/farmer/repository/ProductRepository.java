package co.cambodia.farmer.repository;

import co.cambodia.farmer.domain.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByFarmerId(UUID farmerId);

    List<Product> findByCategoryId(UUID categoryId);

    List<Product> findByFarmerIdAndStatus(UUID farmerId, String status);

    @Query("SELECT p FROM Product p WHERE p.farmer.id = :farmerId AND p.quantity <= p.lowStockThreshold")
    List<Product> findLowStockProductsByFarmerId(@Param("farmerId") UUID farmerId);

    @Query("SELECT p FROM Product p WHERE p.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findByIdWithLock(@Param("id") UUID id);
}
