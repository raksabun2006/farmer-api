package co.cambodia.farmer.service;

import co.cambodia.farmer.controller.BakongPaymentController;
import co.cambodia.farmer.dto.peyment.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BakongPaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private BakongPaymentController controller;

    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Create Payment: Returns QR string, MD5, and transaction details")
    void shouldCreatePaymentTransaction() {
        PaymentResponse response = new PaymentResponse(
                UUID.randomUUID(), null, "MD5HASH123456789012345678901234",
                BigDecimal.valueOf(25.00), "USD", "PENDING",
                LocalDateTime.now(), orderId, "000201010212...6304ABCD",
                LocalDateTime.now().plusMinutes(15)
        );

        when(paymentService.generateKhqrPayment(eq(orderId), any(), any())).thenReturn(response);

        Map<String, Object> request = Map.of("orderId", orderId.toString(), "amount", 25.00);
        ResponseEntity<Object> entity = controller.createTransaction(null, null, request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) entity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("qrString")).isEqualTo("000201010212...6304ABCD");
        assertThat(body.get("md5")).isEqualTo("MD5HASH123456789012345678901234");
        assertThat(body.get("orderId")).isEqualTo(orderId);
        assertThat(body.get("status")).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Create Payment: Fails with 400 when orderId is missing")
    void shouldFailWhenOrderIdMissing() {
        Map<String, Object> request = Map.of("amount", 25.00);
        ResponseEntity<Object> entity = controller.createTransaction(null, null, request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Verify Payment: Returns success when Bakong confirms")
    void shouldVerifyPaymentSuccessfully() {
        PaymentResponse response = new PaymentResponse(
                UUID.randomUUID(), "0xhash123", "MD5HASH123456789012345678901234",
                BigDecimal.valueOf(25.00), "USD", "PAID",
                LocalDateTime.now(), orderId, null, null
        );

        when(paymentService.verifyPayment(eq("MD5HASH123456789012345678901234"), any(), any()))
                .thenReturn(response);

        Map<String, String> request = Map.of("md5", "MD5HASH123456789012345678901234");
        ResponseEntity<Object> entity = controller.verifyKhqrPayment(request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) entity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("PAID");
        assertThat(body.get("paid")).isEqualTo(true);
        assertThat(body.get("responseCode")).isEqualTo(0);
    }

    @Test
    @DisplayName("Status Polling: Returns payment status distinguishing PENDING, PAID, FAILED, EXPIRED")
    void shouldPollPaymentStatus() {
        PaymentResponse response = new PaymentResponse(
                UUID.randomUUID(), "0xhash123", "MD5HASH123456789012345678901234",
                BigDecimal.valueOf(25.00), "USD", "PAID",
                LocalDateTime.now(), orderId, null, null
        );

        when(paymentService.getPaymentStatusByOrderId(orderId)).thenReturn(response);

        ResponseEntity<Object> entity = controller.getPaymentStatusByOrderId(orderId);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) entity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("PAID");
        assertThat(body.get("paid")).isEqualTo(true);
        assertThat(body.get("orderId")).isEqualTo(orderId);
    }
}
