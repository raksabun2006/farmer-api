package co.cambodia.farmer.service;

import co.cambodia.farmer.domain.Order;
import co.cambodia.farmer.dto.peyment.PaymentResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {
    PaymentResponse createPendingPayment(Order order, String md5Hash);
    void updatePaymentSuccess(String md5Hash, String transactionHash);
    PaymentResponse getPaymentByMd5(String md5Hash);

    PaymentResponse generateKhqrPayment(UUID orderId, BigDecimal requestedAmount, String requestedCurrency);
    PaymentResponse verifyPayment(String md5, String transactionId, UUID orderId);
    PaymentResponse getPaymentStatusByOrderId(UUID orderId);
    PaymentResponse getPaymentStatusByMd5(String md5);
}
