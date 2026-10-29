package co.cambodia.farmer.dto.peyment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCreateRequest(
        @NotNull(message = "Order ID link is required")
        UUID orderId,

        @NotBlank(message = "MD5 hash code is required")
        @Size(min = 32, max = 32, message = "MD5 hash must be exactly 32 characters")
        String md5Hash,

        @NotNull(message = "Payment amount configuration is required")
        @Positive(message = "Payment amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Currency type definition is required")
        @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters (USD/KHR)")
        String currency
) {
}
