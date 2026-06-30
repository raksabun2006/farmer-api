package co.cambodia.farmer.dto.peyment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String transactionHash,
        String md5Hash,
        BigDecimal amount,
        String currency,
        String status,
        LocalDateTime createdAt,
        Long orderId
) {
}
