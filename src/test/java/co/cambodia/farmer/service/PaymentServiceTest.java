package co.cambodia.farmer.service;

import co.cambodia.farmer.config.BakongProperties;
import co.cambodia.farmer.domain.Order;
import co.cambodia.farmer.domain.Payment;
import co.cambodia.farmer.dto.peyment.PaymentResponse;
import co.cambodia.farmer.exception.ResourceNotFoundException;
import co.cambodia.farmer.mapper.PaymentMapper;
import co.cambodia.farmer.repository.OrderRepository;
import co.cambodia.farmer.repository.PaymentRepository;
import co.cambodia.farmer.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private FinancialService financialService;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private BakongProperties bakongProperties;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID orderId;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        testOrder = new Order();
        testOrder.setId(orderId);
        testOrder.setTotalAmount(BigDecimal.valueOf(35.00));
        testOrder.setStatus("PENDING");

        lenient().when(bakongProperties.getAccountId()).thenReturn("bun_raksa@bkrt");
        lenient().when(bakongProperties.getMerchantName()).thenReturn("BUN RAKSA");
        lenient().when(bakongProperties.getCity()).thenReturn("Phnom Penh");
        lenient().when(bakongProperties.getToken()).thenReturn("mock-token-xyz");
        lenient().when(bakongProperties.getApiUrl()).thenReturn("https://api-bakong.nbc.gov.kh/v1/check_transaction_by_md5");
    }

    @Test
    @DisplayName("1. KHQR generation with valid payment information")
    void shouldGenerateKhqrWithValidPaymentInfo() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(paymentMapper.toDTO(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            return new PaymentResponse(
                    p.getId(), p.getTransactionHash(), p.getMd5Hash(), p.getAmount(),
                    p.getCurrency(), p.getStatus(), p.getCreatedAt(), orderId, p.getQrString(), p.getExpiresAt()
            );
        });

        PaymentResponse response = paymentService.generateKhqrPayment(orderId, BigDecimal.valueOf(35.00), "USD");

        assertThat(response).isNotNull();
        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(35.00));
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.md5Hash()).isNotBlank().hasSize(32);
        assertThat(response.qrString()).isNotBlank().contains("kh.gov.nbc.bakong");

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getExpiresAt()).isAfter(saved.getCreatedAt());
    }

    @Test
    @DisplayName("2. Invalid sale or invoice")
    void shouldRejectInvalidSaleOrInvoice() {
        UUID nonExistentId = UUID.randomUUID();
        when(orderRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.generateKhqrPayment(nonExistentId, null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order invoice missing");

        testOrder.setStatus("PAID");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        assertThatThrownBy(() -> paymentService.generateKhqrPayment(orderId, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Order is already paid");

        testOrder.setStatus("CANCELLED");
        assertThatThrownBy(() -> paymentService.generateKhqrPayment(orderId, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Order has been cancelled");
    }

    @Test
    @DisplayName("3. Duplicate payment requests: reuse active pending payment")
    void shouldPreventDuplicateActivePaymentRequests() {
        Payment activePayment = new Payment();
        activePayment.setId(UUID.randomUUID());
        activePayment.setOrder(testOrder);
        activePayment.setAmount(BigDecimal.valueOf(35.00));
        activePayment.setCurrency("USD");
        activePayment.setStatus("PENDING");
        activePayment.setMd5Hash("12345678901234567890123456789012");
        activePayment.setCreatedAt(LocalDateTime.now());
        activePayment.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)).thenReturn(Optional.of(activePayment));
        when(paymentMapper.toDTO(activePayment)).thenReturn(
                new PaymentResponse(activePayment.getId(), null, activePayment.getMd5Hash(), activePayment.getAmount(),
                        activePayment.getCurrency(), "PENDING", activePayment.getCreatedAt(), orderId)
        );

        PaymentResponse response = paymentService.generateKhqrPayment(orderId, null, "USD");

        assertThat(response).isNotNull();
        assertThat(response.md5Hash()).isEqualTo(activePayment.getMd5Hash());
        // Verify no second payment entity was created/saved
        verify(paymentRepository, never()).save(argThat(p -> p != activePayment));
    }

    @Test
    @DisplayName("4. Pending payment status: Bakong reports unconfirmed")
    void shouldHandlePendingPaymentStatusWhenBakongReportsUnconfirmed() {
        String md5 = "ABCDEF1234567890ABCDEF1234567890";
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrder(testOrder);
        payment.setAmount(BigDecimal.valueOf(35.00));
        payment.setCurrency("USD");
        payment.setStatus("PENDING");
        payment.setMd5Hash(md5);
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(paymentRepository.findByMd5HashIgnoreCase(md5)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByIdWithLock(payment.getId())).thenReturn(Optional.of(payment));

        Map<String, Object> bakongResponseBody = Map.of("responseCode", 1, "responseMessage", "Transaction not found");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(bakongResponseBody));
        when(paymentMapper.toDTO(payment)).thenReturn(
                new PaymentResponse(payment.getId(), null, md5, payment.getAmount(), "USD", "PENDING", payment.getCreatedAt(), orderId)
        );

        PaymentResponse response = paymentService.verifyPayment(md5, null, null);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(testOrder.getStatus()).isEqualTo("PENDING");
        verify(orderRepository, never()).save(any());
        verify(financialService, never()).recordOrderPaymentIncome(any(), any());
    }

    @Test
    @DisplayName("5. Successfully verified payment: completes sale and records income")
    void shouldCompleteSaleUponSuccessfullyVerifiedPayment() {
        String md5 = "ABCDEF1234567890ABCDEF1234567890";
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrder(testOrder);
        payment.setAmount(BigDecimal.valueOf(35.00));
        payment.setCurrency("USD");
        payment.setStatus("PENDING");
        payment.setMd5Hash(md5);
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(paymentRepository.findByMd5HashIgnoreCase(md5)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByIdWithLock(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> data = Map.of(
                "hash", "0xdeadbeef1234567890abcdef",
                "amount", 35.00,
                "currency", "USD"
        );
        Map<String, Object> bakongResponse = Map.of(
                "responseCode", 0,
                "responseMessage", "Success",
                "data", data
        );
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(bakongResponse));

        when(paymentMapper.toDTO(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            return new PaymentResponse(p.getId(), p.getTransactionHash(), p.getMd5Hash(), p.getAmount(),
                    p.getCurrency(), p.getStatus(), p.getCreatedAt(), orderId);
        });

        PaymentResponse response = paymentService.verifyPayment(md5, null, null);

        assertThat(response.status()).isEqualTo("PAID");
        assertThat(payment.getStatus()).isEqualTo("PAID");
        assertThat(payment.getTransactionHash()).isEqualTo("0xdeadbeef1234567890abcdef");
        assertThat(testOrder.getStatus()).isEqualTo("PAID");

        verify(orderRepository).save(testOrder);
        verify(financialService).recordOrderPaymentIncome(eq(testOrder), eq(payment));
    }

    @Test
    @DisplayName("6. Failed or expired payment: marks EXPIRED when past expiration")
    void shouldMarkPaymentExpiredWhenPastExpiration() {
        String md5 = "EXPIREDMD51234567890123456789012";
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrder(testOrder);
        payment.setAmount(BigDecimal.valueOf(35.00));
        payment.setCurrency("USD");
        payment.setStatus("PENDING");
        payment.setMd5Hash(md5);
        payment.setExpiresAt(LocalDateTime.now().minusMinutes(5)); // Expired 5 mins ago

        when(paymentRepository.findByMd5HashIgnoreCase(md5)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByIdWithLock(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> bakongResponse = Map.of("responseCode", 1, "responseMessage", "Transaction not found");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(bakongResponse));

        when(paymentMapper.toDTO(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            return new PaymentResponse(p.getId(), null, p.getMd5Hash(), p.getAmount(),
                    p.getCurrency(), p.getStatus(), p.getCreatedAt(), orderId);
        });

        PaymentResponse response = paymentService.verifyPayment(md5, null, null);

        assertThat(response.status()).isEqualTo("EXPIRED");
        assertThat(payment.getStatus()).isEqualTo("EXPIRED");
        verify(orderRepository, never()).save(any());
        verify(financialService, never()).recordOrderPaymentIncome(any(), any());
    }

    @Test
    @DisplayName("7. Bakong API timeout or unavailable service: keeps PENDING, never marks PAID")
    void shouldHandleBakongTimeoutGracefully() {
        String md5 = "TIMEOUTMD51234567890123456789012";
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrder(testOrder);
        payment.setAmount(BigDecimal.valueOf(35.00));
        payment.setCurrency("USD");
        payment.setStatus("PENDING");
        payment.setMd5Hash(md5);
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(paymentRepository.findByMd5HashIgnoreCase(md5)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByIdWithLock(payment.getId())).thenReturn(Optional.of(payment));

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Connection timed out reading from Bakong"));

        when(paymentMapper.toDTO(payment)).thenReturn(
                new PaymentResponse(payment.getId(), null, md5, payment.getAmount(), "USD", "PENDING", payment.getCreatedAt(), orderId)
        );

        PaymentResponse response = paymentService.verifyPayment(md5, null, null);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(payment.getStatus()).isEqualTo("PENDING");
        verify(orderRepository, never()).save(any());
        verify(financialService, never()).recordOrderPaymentIncome(any(), any());
    }

    @Test
    @DisplayName("8. Repeated payment verification without duplicate sale completion: idempotent")
    void shouldEnsureIdempotencyOnRepeatedVerification() {
        String md5 = "ALREADYPAIDMD5123456789012345678";
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrder(testOrder);
        payment.setAmount(BigDecimal.valueOf(35.00));
        payment.setCurrency("USD");
        payment.setStatus("PAID"); // Already paid
        payment.setTransactionHash("EXISTING_HASH");
        payment.setMd5Hash(md5);

        when(paymentRepository.findByMd5HashIgnoreCase(md5)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByIdWithLock(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentMapper.toDTO(payment)).thenReturn(
                new PaymentResponse(payment.getId(), "EXISTING_HASH", md5, payment.getAmount(), "USD", "PAID", payment.getCreatedAt(), orderId)
        );

        PaymentResponse response = paymentService.verifyPayment(md5, null, null);

        assertThat(response.status()).isEqualTo("PAID");
        // Verify Bakong API was NOT called and sale completion side effects were NOT repeated
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        verify(orderRepository, never()).save(any());
        verify(financialService, never()).recordOrderPaymentIncome(any(), any());
    }

    @Test
    @DisplayName("9. Invalid or mismatched payment amount")
    void shouldRejectMismatchedAmount() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // Order is 35.00, client passes 10.00
        assertThatThrownBy(() -> paymentService.generateKhqrPayment(orderId, BigDecimal.valueOf(10.00), "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match order total amount");

        // Bakong returns 10.00 instead of 35.00
        String md5 = "MISMATCHMD512345678901234567890";
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrder(testOrder);
        payment.setAmount(BigDecimal.valueOf(35.00));
        payment.setCurrency("USD");
        payment.setStatus("PENDING");
        payment.setMd5Hash(md5);
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(paymentRepository.findByMd5HashIgnoreCase(md5)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByIdWithLock(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> data = Map.of(
                "hash", "0xsomehash",
                "amount", 10.00, // Mismatched!
                "currency", "USD"
        );
        Map<String, Object> bakongResponse = Map.of(
                "responseCode", 0,
                "data", data
        );
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(bakongResponse));
        when(paymentMapper.toDTO(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            return new PaymentResponse(p.getId(), null, md5, p.getAmount(), "USD", p.getStatus(), p.getCreatedAt(), orderId);
        });

        PaymentResponse response = paymentService.verifyPayment(md5, null, null);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(payment.getStatus()).isEqualTo("FAILED");
        verify(orderRepository, never()).save(any());
        verify(financialService, never()).recordOrderPaymentIncome(any(), any());
    }

    @Test
    @DisplayName("10. Database transaction rollback when payment completion fails")
    void shouldPropagateExceptionWhenPaymentCompletionFails() {
        String md5 = "ROLLBACKMD5123456789012345678901";
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrder(testOrder);
        payment.setAmount(BigDecimal.valueOf(35.00));
        payment.setCurrency("USD");
        payment.setStatus("PENDING");
        payment.setMd5Hash(md5);
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(paymentRepository.findByMd5HashIgnoreCase(md5)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByIdWithLock(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> bakongResponse = Map.of(
                "responseCode", 0,
                "data", Map.of("hash", "0xhash", "amount", 35.00, "currency", "USD")
        );
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(bakongResponse));

        doThrow(new RuntimeException("Database ledger write failed"))
                .when(financialService).recordOrderPaymentIncome(any(), any());

        assertThatThrownBy(() -> paymentService.verifyPayment(md5, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database ledger write failed");
    }
}
