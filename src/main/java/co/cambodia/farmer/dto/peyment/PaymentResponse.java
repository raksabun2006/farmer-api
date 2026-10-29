package co.cambodia.farmer.dto.peyment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String transactionHash,
        String md5Hash,
        BigDecimal amount,
        String currency,
        String status,
        LocalDateTime createdAt,
        UUID orderId,
        String qrString,
        LocalDateTime expiresAt
) {
    public PaymentResponse(
            UUID id,
            String transactionHash,
            String md5Hash,
            BigDecimal amount,
            String currency,
            String status,
            LocalDateTime createdAt,
            UUID orderId
    ) {
        this(id, transactionHash, md5Hash, amount, currency, status, createdAt, orderId, null, null);
    }
}
