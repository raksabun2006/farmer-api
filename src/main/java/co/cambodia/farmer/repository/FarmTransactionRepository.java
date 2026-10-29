package co.cambodia.farmer.repository;

import co.cambodia.farmer.domain.FarmTransaction;
import co.cambodia.farmer.domain.TransactionCategory;
import co.cambodia.farmer.domain.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FarmTransactionRepository extends JpaRepository<FarmTransaction, UUID> {

    Page<FarmTransaction> findByFarmerIdOrderByTransactionDateDesc(UUID farmerId, Pageable pageable);

    Page<FarmTransaction> findByFarmerIdAndTypeOrderByTransactionDateDesc(
            UUID farmerId, TransactionType type, Pageable pageable
    );

    Page<FarmTransaction> findByFarmerIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            UUID farmerId, LocalDate startDate, LocalDate endDate, Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(t.amountInUsd), 0) FROM FarmTransaction t " +
           "WHERE t.farmer.id = :farmerId AND t.type = :type " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountInUsdByFarmerIdAndTypeAndDateRange(
            @Param("farmerId") UUID farmerId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(t.amountInUsd), 0) FROM FarmTransaction t " +
           "WHERE t.farmer.id = :farmerId AND t.type = :type")
    BigDecimal sumTotalAmountInUsdByFarmerIdAndType(
            @Param("farmerId") UUID farmerId,
            @Param("type") TransactionType type
    );

    @Query("SELECT t.category, COALESCE(SUM(t.amountInUsd), 0) FROM FarmTransaction t " +
           "WHERE t.farmer.id = :farmerId AND t.type = :type " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category")
    List<Object[]> sumAmountByCategoryAndDateRange(
            @Param("farmerId") UUID farmerId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
