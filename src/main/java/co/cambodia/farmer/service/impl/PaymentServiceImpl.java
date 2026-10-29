package co.cambodia.farmer.service.impl;

import co.cambodia.farmer.config.BakongProperties;
import co.cambodia.farmer.domain.Order;
import co.cambodia.farmer.domain.Payment;
import co.cambodia.farmer.dto.peyment.PaymentResponse;
import co.cambodia.farmer.exception.ResourceNotFoundException;
import co.cambodia.farmer.mapper.PaymentMapper;
import co.cambodia.farmer.repository.OrderRepository;
import co.cambodia.farmer.repository.PaymentRepository;
import co.cambodia.farmer.service.FinancialService;
import co.cambodia.farmer.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final FinancialService financialService;
    private final PaymentMapper paymentMapper;
    private final BakongProperties bakongProperties;
    private final RestTemplate restTemplate;

    @Override
    @Transactional
    public PaymentResponse createPendingPayment(Order order, String md5Hash) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null for creating pending payment");
        }
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMd5Hash(md5Hash);
        payment.setAmount(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
        payment.setCurrency("USD");
        payment.setStatus("PENDING");
        payment.setCreatedAt(LocalDateTime.now());
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toDTO(savedPayment);
    }

    @Override
    @Transactional
    public void updatePaymentSuccess(String md5Hash, String transactionHash) {
        Payment payment = paymentRepository.findByMd5HashIgnoreCase(md5Hash)
                .orElseThrow(() -> new RuntimeException("Transaction records not found for MD5: " + md5Hash));

        completePaymentInternal(payment, transactionHash);
    }
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByMd5(String md5Hash) {
        Payment payment = paymentRepository.findByMd5HashIgnoreCase(md5Hash)
                .orElseThrow(() -> new RuntimeException("Payment secure entity not found"));
        return paymentMapper.toDTO(payment);
    }

    @Override
    @Transactional
    public PaymentResponse generateKhqrPayment(UUID orderId, BigDecimal requestedAmount, String requestedCurrency) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required to generate payment");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order invoice missing with id: " + orderId));

        if ("PAID".equalsIgnoreCase(order.getStatus()) || "COMPLETED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Order is already paid");
        }

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Order has been cancelled");
        }

        BigDecimal orderAmount = order.getTotalAmount();
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Order total amount must be greater than zero");
        }

        if (requestedAmount != null && requestedAmount.compareTo(orderAmount) != 0) {
            throw new IllegalArgumentException(
                    String.format(Locale.US, "Payment amount (%.2f) does not match order total amount (%.2f)",
                            requestedAmount, orderAmount)
            );
        }
        String currency = (requestedCurrency != null && !requestedCurrency.isBlank())
                ? requestedCurrency.trim().toUpperCase()
                : "USD";

        // Prevent duplicate active payment requests for the same order
        Optional<Payment> existingPaymentOpt = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId);
        if (existingPaymentOpt.isPresent()) {
            Payment existing = existingPaymentOpt.get();
            if ("PAID".equalsIgnoreCase(existing.getStatus()) || "SUCCESS".equalsIgnoreCase(existing.getStatus())) {
                throw new IllegalStateException("Payment for this order has already succeeded");
            }
            if ("PENDING".equalsIgnoreCase(existing.getStatus())) {
                if (existing.getExpiresAt() != null && LocalDateTime.now().isBefore(existing.getExpiresAt())) {
                    return paymentMapper.toDTO(existing);
                } else if (existing.getExpiresAt() != null) {
                    existing.setStatus("EXPIRED");
                    paymentRepository.save(existing);
                }
            }
        }

        String txId = "T" + (System.currentTimeMillis() / 1000);
        String rawQrPayload = generateRawKhqr(
                bakongProperties.getAccountId(),
                bakongProperties.getMerchantName(),
                bakongProperties.getCity(),
                orderAmount,
                currency,
                txId
        );

        String md5Hash = DigestUtils.md5DigestAsHex(rawQrPayload.getBytes(StandardCharsets.UTF_8)).toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(15);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(orderAmount);
        payment.setCurrency(currency);
        payment.setMd5Hash(md5Hash);
        payment.setQrString(rawQrPayload);
        payment.setStatus("PENDING");
        payment.setCreatedAt(now);
        payment.setExpiresAt(expiresAt);

        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toDTO(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(String md5, String transactionId, UUID orderId) {
        Payment payment = null;
        if (md5 != null && !md5.isBlank()) {
            payment = paymentRepository.findByMd5HashIgnoreCase(md5.trim()).orElse(null);
        }
        if (payment == null && orderId != null) {
            payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId).orElse(null);
        }
        if (payment == null) {
            throw new ResourceNotFoundException("Payment record not found for verification");
        }

        // Concurrency protection: acquire lock on the payment record
        payment = paymentRepository.findByIdWithLock(payment.getId()).orElse(payment);

        // Idempotency: return immediately if already completed
        if ("PAID".equalsIgnoreCase(payment.getStatus()) || "SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            return paymentMapper.toDTO(payment);
        }

        // Check if expired prior to verification
        boolean isExpired = payment.getExpiresAt() != null && LocalDateTime.now().isAfter(payment.getExpiresAt());

        Map<String, Object> bakongResponse = callBakongApi(payment.getMd5Hash());
        if (bakongResponse == null) {
            log.warn("Bakong verification service unavailable or timed out for MD5: {}", payment.getMd5Hash());
            return paymentMapper.toDTO(payment);
        }

        Object codeObj = bakongResponse.get("responseCode");
        boolean isSuccess = codeObj != null && ("0".equals(String.valueOf(codeObj)) || Integer.valueOf(0).equals(codeObj));

        if (isSuccess) {
            Map<?, ?> data = (bakongResponse.get("data") instanceof Map<?, ?> map) ? map : null;
            if (data != null) {
                if (data.containsKey("amount") && data.get("amount") != null) {
                    try {
                        BigDecimal confirmedAmount = new BigDecimal(String.valueOf(data.get("amount")));
                        if (confirmedAmount.compareTo(payment.getAmount()) != 0) {
                            log.error("Payment amount mismatch: expected {}, received {}", payment.getAmount(), confirmedAmount);
                            payment.setStatus("FAILED");
                            return paymentMapper.toDTO(paymentRepository.save(payment));
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Invalid amount format in Bakong response: {}", data.get("amount"));
                    }
                }
                if (data.containsKey("currency") && data.get("currency") != null) {
                    String confirmedCurrency = String.valueOf(data.get("currency"));
                    if (!payment.getCurrency().equalsIgnoreCase(confirmedCurrency)) {
                        log.error("Payment currency mismatch: expected {}, received {}", payment.getCurrency(), confirmedCurrency);
                        payment.setStatus("FAILED");
                        return paymentMapper.toDTO(paymentRepository.save(payment));
                    }
                }
            }

            String txHash = null;
            if (data != null && data.containsKey("hash") && data.get("hash") != null) {
                txHash = String.valueOf(data.get("hash"));
            } else if (transactionId != null && !transactionId.isBlank()) {
                txHash = transactionId;
            } else {
                txHash = "KHQR_CONFIRMED";
            }

            return completePaymentInternal(payment, txHash);
        } else {
            if (isExpired) {
                payment.setStatus("EXPIRED");
                Payment saved = paymentRepository.save(payment);
                return paymentMapper.toDTO(saved);
            }
            return paymentMapper.toDTO(payment);
        }
    }

    @Override
    @Transactional
    public PaymentResponse getPaymentStatusByOrderId(UUID orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for order: " + orderId));

        if ("PAID".equalsIgnoreCase(payment.getStatus()) || "SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            return paymentMapper.toDTO(payment);
        }

        // Active status polling: verify with Bakong server
        return verifyPayment(payment.getMd5Hash(), null, orderId);
    }

    @Override
    @Transactional
    public PaymentResponse getPaymentStatusByMd5(String md5) {
        if (md5 == null || md5.isBlank()) {
            throw new IllegalArgumentException("MD5 hash cannot be empty");
        }

        Payment payment = paymentRepository.findByMd5HashIgnoreCase(md5.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for MD5: " + md5));

        if ("PAID".equalsIgnoreCase(payment.getStatus()) || "SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            return paymentMapper.toDTO(payment);
        }

        return verifyPayment(md5.trim(), null, null);
    }

    @Transactional
    public PaymentResponse completePaymentInternal(Payment payment, String transactionHash) {
        if ("PAID".equalsIgnoreCase(payment.getStatus()) || "SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            return paymentMapper.toDTO(payment);
        }

        payment.setStatus("PAID");
        payment.setTransactionHash(transactionHash != null && !transactionHash.isBlank() ? transactionHash : "KHQR_CONFIRMED");
        Payment savedPayment = paymentRepository.save(payment);

        Order order = payment.getOrder();
        if (order != null && !"PAID".equalsIgnoreCase(order.getStatus())) {
            order.setStatus("PAID");
            orderRepository.save(order);
            financialService.recordOrderPaymentIncome(order, savedPayment);
        }

        return paymentMapper.toDTO(savedPayment);
    }

    private Map<String, Object> callBakongApi(String md5) {
        try {
            String url = bakongProperties.getApiUrl();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (bakongProperties.getToken() != null && !bakongProperties.getToken().isBlank()) {
                headers.setBearerAuth(bakongProperties.getToken().trim());
            }

            Map<String, String> body = Map.of("md5", md5);
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Error communicating with Bakong server: {}", e.getMessage());
            return null;
        }
    }

    public static String generateRawKhqr(
            String bakongAccountId,
            String merchantName,
            String merchantCity,
            BigDecimal amount,
            String currency,
            String transactionRef
    ) {
        String p00 = "000201";
        String p01 = "010212"; // Dynamic QR

        String merchantIdClean = (bakongAccountId != null && !bakongAccountId.isBlank())
                ? bakongAccountId.trim()
                : "merchant@bkrt";
        String bakongId = merchantIdClean.contains("@") ? merchantIdClean : merchantIdClean + "@aba";

        String subTag00 = "0017kh.gov.nbc.bakong";
        String subTag01 = "01" + String.format("%02d", bakongId.length()) + bakongId;
        String tag29Data = subTag00 + subTag01;
        String tag29 = "29" + String.format("%02d", tag29Data.length()) + tag29Data;

        String tag52 = "52045999";

        String currCode = "KHR".equalsIgnoreCase(currency) ? "116" : "840";
        String tag53 = "5303" + currCode;

        String formattedAmount;
        if ("KHR".equalsIgnoreCase(currency)) {
            formattedAmount = String.format(Locale.US, "%.0f", amount);
        } else {
            formattedAmount = String.format(Locale.US, "%.2f", amount);
        }
        String tag54 = "54" + String.format("%02d", formattedAmount.length()) + formattedAmount;

        String tag58 = "5802KH";

        String mName = (merchantName != null && !merchantName.isBlank()) ? merchantName.trim() : "BUN RAKSA";
        String tag59 = "59" + String.format("%02d", mName.length()) + mName;

        String mCity = (merchantCity != null && !merchantCity.isBlank()) ? merchantCity.trim() : "Phnom Penh";
        String tag60 = "60" + String.format("%02d", mCity.length()) + mCity;

        String refSubTag01 = "01" + String.format("%02d", transactionRef.length()) + transactionRef;
        String tag62 = "62" + String.format("%02d", refSubTag01.length()) + refSubTag01;

        String payload = p00 + p01 + tag29 + tag52 + tag53 + tag54 + tag58 + tag59 + tag60 + tag62 + "6304";
        String crc = calculateCRC16(payload);
        return payload + crc;
    }

    public static String calculateCRC16(String input) {
        int crc = 0xFFFF;
        int polynomial = 0x1021;
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }
        crc &= 0xFFFF;
        return String.format("%04X", crc);
    }
}
