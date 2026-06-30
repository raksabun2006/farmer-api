package co.cambodia.farmer.service;


import co.cambodia.farmer.domain.Order;
import co.cambodia.farmer.dto.peyment.PaymentResponse;

public interface PaymentService {
    PaymentResponse createPendingPayment(Order order, String md5Hash);
    void updatePaymentSuccess(String md5Hash, String transactionHash);
    PaymentResponse getPaymentByMd5(String md5Hash);
}