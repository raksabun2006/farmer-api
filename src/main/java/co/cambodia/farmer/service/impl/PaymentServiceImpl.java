package co.cambodia.farmer.service.impl;

import co.cambodia.farmer.domain.Order;
import co.cambodia.farmer.domain.Payment;
import co.cambodia.farmer.dto.peyment.PaymentResponse;
import co.cambodia.farmer.mapper.PaymentMapper;
import co.cambodia.farmer.repository.PaymentRepository;
import co.cambodia.farmer.service.PaymentService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    public PaymentServiceImpl(PaymentRepository paymentRepository, PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
    }

    @Override
    public PaymentResponse createPendingPayment(Order order, String md5Hash) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMd5Hash(md5Hash);
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency("USD");
        payment.setStatus("PENDING");

        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toDTO(savedPayment);
    }

    @Override
    public void updatePaymentSuccess(String md5Hash, String transactionHash) {
        Payment payment = paymentRepository.findByMd5Hash(md5Hash)
                .orElseThrow(() -> new RuntimeException("Transaction records not found for MD5: " + md5Hash));

        payment.setStatus("SUCCESS");
        payment.setTransactionHash(transactionHash);

        Payment updatedPayment = paymentRepository.save(payment);
        paymentMapper.toDTO(updatedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse getPaymentByMd5(String md5Hash) {
        Payment payment = paymentRepository.findByMd5Hash(md5Hash)
                .orElseThrow(() -> new RuntimeException("Payment secure entity not found"));
        return paymentMapper.toDTO(payment);
    }
}