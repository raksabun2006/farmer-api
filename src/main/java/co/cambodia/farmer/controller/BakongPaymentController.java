package co.cambodia.farmer.controller;

import co.cambodia.farmer.config.BakongProperties;
import co.cambodia.farmer.domain.Order;
import co.cambodia.farmer.dto.peyment.PaymentResponse;
import co.cambodia.farmer.repository.OrderRepository;
import co.cambodia.farmer.service.PaymentService;
import co.cambodia.farmer.service.impl.PaymentServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class BakongPaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;
    private final BakongProperties bakongProperties;
    private final RestTemplate restTemplate;

    @PostMapping({
            "/api/payment/create",
            "/api/v1/payments/create",
            "/api/v1/sales/{saleId}/payment/create",
            "/api/v1/orders/{orderId}/payment/create"
    })
    public ResponseEntity<Object> createTransaction(
            @PathVariable(value = "saleId", required = false) UUID pathSaleId,
            @PathVariable(value = "orderId", required = false) UUID pathOrderId,
            @RequestBody(required = false) Map<String, Object> request
    ) {
        try {
            UUID orderId = pathSaleId != null ? pathSaleId : pathOrderId;
            BigDecimal amount = null;
            String currency = null;

            if (request != null) {
                if (orderId == null) {
                    if (request.containsKey("orderId") && request.get("orderId") != null && !request.get("orderId").toString().isBlank()) {
                        orderId = UUID.fromString(request.get("orderId").toString().trim());
                    } else if (request.containsKey("order_id") && request.get("order_id") != null && !request.get("order_id").toString().isBlank()) {
                        orderId = UUID.fromString(request.get("order_id").toString().trim());
                    } else if (request.containsKey("saleId") && request.get("saleId") != null && !request.get("saleId").toString().isBlank()) {
                        orderId = UUID.fromString(request.get("saleId").toString().trim());
                    }
                }
                if (request.containsKey("amount") && request.get("amount") != null) {
                    try {
                        amount = new BigDecimal(request.get("amount").toString().trim());
                    } catch (NumberFormatException nfe) {
                        return ResponseEntity.badRequest().body(Map.of("error", "The 'amount' property must be a valid number."));
                    }
                }
                if (request.containsKey("currency") && request.get("currency") != null) {
                    currency = request.get("currency").toString().trim();
                }
            }

            // Fallback: If orderId is not explicitly sent, attempt to find a matching pending order
            if (orderId == null && amount != null) {
                final BigDecimal targetAmount = amount;
                Optional<Order> pendingOrder = orderRepository.findAll().stream()
                        .filter(o -> "PENDING".equalsIgnoreCase(o.getStatus()))
                        .filter(o -> o.getTotalAmount() != null && o.getTotalAmount().compareTo(targetAmount) == 0)
                        .max(Comparator.comparing(Order::getId, (a, b) -> 0)); // latest
                if (pendingOrder.isPresent()) {
                    orderId = pendingOrder.get().getId();
                }
            }

            if (orderId != null) {
                PaymentResponse payment = paymentService.generateKhqrPayment(orderId, amount, currency);

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("qrString", payment.qrString());
                responseData.put("md5", payment.md5Hash());
                responseData.put("md5Hash", payment.md5Hash());
                responseData.put("transactionId", payment.id());
                responseData.put("id", payment.id());
                responseData.put("orderId", payment.orderId());
                responseData.put("saleId", payment.orderId());
                responseData.put("amount", payment.amount());
                responseData.put("currency", payment.currency());
                responseData.put("status", payment.status());
                responseData.put("createdAt", payment.createdAt());
                responseData.put("expiresAt", payment.expiresAt());

                return ResponseEntity.ok(responseData);
            }

            // Standalone fallback: If no orderId is known, generate valid dynamic KHQR for the amount
            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                String txId = "T" + (System.currentTimeMillis() / 1000);
                String curr = (currency != null && !currency.isBlank()) ? currency.trim().toUpperCase() : "USD";
                String rawQrPayload = PaymentServiceImpl.generateRawKhqr(
                        bakongProperties.getAccountId(),
                        bakongProperties.getMerchantName(),
                        bakongProperties.getCity(),
                        amount,
                        curr,
                        txId
                );
                String md5Hash = DigestUtils.md5DigestAsHex(rawQrPayload.getBytes(StandardCharsets.UTF_8)).toUpperCase();

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("qrString", rawQrPayload);
                responseData.put("md5", md5Hash);
                responseData.put("md5Hash", md5Hash);
                responseData.put("transactionId", txId);
                responseData.put("amount", amount);
                responseData.put("currency", curr);
                responseData.put("status", "PENDING");

                return ResponseEntity.ok(responseData);
            }

            return ResponseEntity.badRequest().body(Map.of("error", "The 'orderId' (or 'amount') is required in order to initiate payment."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[KHQR CREATE ERROR] {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping({"/api/payment/verify", "/api/v1/payments/verify"})
    public ResponseEntity<Object> verifyKhqrPayment(@RequestBody(required = false) Map<String, String> request) {
        try {
            String md5 = null;
            String transactionId = null;
            UUID orderId = null;

            if (request != null) {
                if (request.containsKey("md5") && request.get("md5") != null && !request.get("md5").isBlank()) {
                    md5 = request.get("md5").trim();
                } else if (request.containsKey("md5Hash") && request.get("md5Hash") != null && !request.get("md5Hash").isBlank()) {
                    md5 = request.get("md5Hash").trim();
                }
                if (request.containsKey("transactionId")) {
                    transactionId = request.get("transactionId");
                }
                if (request.containsKey("orderId") && request.get("orderId") != null && !request.get("orderId").isBlank()) {
                    orderId = UUID.fromString(request.get("orderId").trim());
                } else if (request.containsKey("saleId") && request.get("saleId") != null && !request.get("saleId").isBlank()) {
                    orderId = UUID.fromString(request.get("saleId").trim());
                }
            }

            if ((md5 == null || md5.isBlank()) && orderId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "md5 or orderId parameter is required"));
            }

            try {
                PaymentResponse response = paymentService.verifyPayment(md5, transactionId, orderId);

                boolean isPaid = "PAID".equalsIgnoreCase(response.status()) || "SUCCESS".equalsIgnoreCase(response.status());

                Map<String, Object> result = new HashMap<>();
                result.put("responseCode", isPaid ? 0 : 1);
                result.put("responseMessage", isPaid ? "Success" : "Payment pending or unconfirmed");
                result.put("status", response.status());
                result.put("paid", isPaid);
                result.put("md5", response.md5Hash());
                result.put("orderId", response.orderId());
                result.put("saleId", response.orderId());
                result.put("amount", response.amount());
                result.put("currency", response.currency());
                result.put("transactionHash", response.transactionHash());

                return ResponseEntity.ok(result);
            } catch (Exception ex) {
                // If payment record was not in database (standalone QR), check Bakong API directly
                if (md5 != null) {
                    Map<String, Object> bakongRes = callBakongDirect(md5);
                    if (bakongRes != null && ("0".equals(String.valueOf(bakongRes.get("responseCode"))) || Integer.valueOf(0).equals(bakongRes.get("responseCode")))) {
                        return ResponseEntity.ok(Map.of(
                                "responseCode", 0,
                                "responseMessage", "Success",
                                "status", "PAID",
                                "paid", true,
                                "md5", md5
                        ));
                    }
                    return ResponseEntity.ok(Map.of(
                            "responseCode", 1,
                            "responseMessage", "Payment pending or unconfirmed",
                            "status", "PENDING",
                            "paid", false,
                            "md5", md5
                    ));
                }
                throw ex;
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[KHQR VERIFY ERROR] {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping({
            "/api/payment/status/{orderId}",
            "/api/v1/orders/{orderId}/payment/status",
            "/api/v1/sales/{orderId}/payment/status"
    })
    public ResponseEntity<Object> getPaymentStatusByOrderId(@PathVariable UUID orderId) {
        try {
            PaymentResponse response = paymentService.getPaymentStatusByOrderId(orderId);
            boolean isPaid = "PAID".equalsIgnoreCase(response.status()) || "SUCCESS".equalsIgnoreCase(response.status());

            Map<String, Object> result = new HashMap<>();
            result.put("status", response.status());
            result.put("paid", isPaid);
            result.put("orderId", response.orderId());
            result.put("saleId", response.orderId());
            result.put("md5", response.md5Hash());
            result.put("amount", response.amount());
            result.put("currency", response.currency());
            result.put("transactionHash", response.transactionHash());
            result.put("createdAt", response.createdAt());
            result.put("expiresAt", response.expiresAt());

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[KHQR STATUS ERROR] {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/payment/status")
    public ResponseEntity<Object> getPaymentStatus(
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) String md5
    ) {
        try {
            if (orderId == null && (md5 == null || md5.isBlank())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Either orderId or md5 query parameter must be provided"));
            }

            PaymentResponse response;
            if (orderId != null) {
                response = paymentService.getPaymentStatusByOrderId(orderId);
            } else {
                response = paymentService.getPaymentStatusByMd5(md5);
            }

            boolean isPaid = "PAID".equalsIgnoreCase(response.status()) || "SUCCESS".equalsIgnoreCase(response.status());

            Map<String, Object> result = new HashMap<>();
            result.put("status", response.status());
            result.put("paid", isPaid);
            result.put("orderId", response.orderId());
            result.put("saleId", response.orderId());
            result.put("md5", response.md5Hash());
            result.put("amount", response.amount());
            result.put("currency", response.currency());
            result.put("transactionHash", response.transactionHash());
            result.put("createdAt", response.createdAt());
            result.put("expiresAt", response.expiresAt());

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[KHQR STATUS ERROR] {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> callBakongDirect(String md5) {
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
        } catch (Exception e) {
            log.warn("Direct Bakong check failed for MD5: {}", md5);
            return null;
        }
    }
}
