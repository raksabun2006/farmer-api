package co.cambodia.farmer.repository;

import co.cambodia.farmer.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,Long> {
    Optional<Payment> findByMd5Hash(String md5Hash);
}
