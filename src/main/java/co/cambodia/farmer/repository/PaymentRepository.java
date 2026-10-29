package co.cambodia.farmer.repository;

import co.cambodia.farmer.domain.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByMd5Hash(String md5Hash);

    @Query("SELECT p FROM Payment p WHERE LOWER(p.md5Hash) = LOWER(:md5Hash)")
    Optional<Payment> findByMd5HashIgnoreCase(@Param("md5Hash") String md5Hash);

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE LOWER(p.md5Hash) = LOWER(:md5Hash)")
    Optional<Payment> findByMd5HashWithLock(@Param("md5Hash") String md5Hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdWithLock(@Param("id") UUID id);
}
