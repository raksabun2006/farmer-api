package co.cambodia.farmer.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import co.cambodia.farmer.service.PaymentService;

import java.util.HashMap;
import java.util.Map;
import org.springframework.util.DigestUtils;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "http://localhost:5173")
public class BakongPaymentController {
    private final PaymentService paymentService;
    private final RestTemplate restTemplate;

    @Value("${bakong.api.token}")
    private String bakongToken;

    @Value("${bakong.account.id}")
    private String merchantIdConfig;

    @Value("${bakong.merchant.name}")
    private String merchantName;

    @Value("${bakong.merchant.city}")
    private String merchantCity;

    public BakongPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
        this.restTemplate = new RestTemplate();
    }

    private String calculateCRC16(String input) {
        int crc = 0xFFFF;
        int polynomial = 0x1021;
        byte[] bytes = input.getBytes();
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

    @PostMapping("/create")
    public ResponseEntity<Object> createTransaction(@RequestBody Map<String, Object> checkoutRequest) {
        try {
            // FIX: បន្ថែមការការពារដើម្បីកុំឱ្យបោះកំហុស 500 នៅពេល Request Body មិនមាន "amount"
            if (checkoutRequest == null || !checkoutRequest.containsKey("amount") || checkoutRequest.get("amount") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "The 'amount' property is required in the request body."));
            }

            double amount;
            try {
                amount = Double.parseDouble(String.valueOf(checkoutRequest.get("amount")));
            } catch (NumberFormatException nfe) {
                return ResponseEntity.badRequest().body(Map.of("error", "The 'amount' property must be a valid number."));
            }

            String txId = "T" + (System.currentTimeMillis() / 1000);

            String p00 = "000201";
            String p01 = "010211";

            String merchantIdClean = merchantIdConfig.trim();
            String bakongId = merchantIdClean.contains("@") ? merchantIdClean : merchantIdClean + "@aba";

            String subTag00 = "0017kh.gov.nbc.bakong";
            String subTag01 = "01" + String.format("%02d", bakongId.length()) + bakongId;
            String tag29Data = subTag00 + subTag01;
            String tag29 = "29" + String.format("%02d", tag29Data.length()) + tag29Data;

            String tag52 = "52045999";
            String tag53 = "5303840";

            String formattedAmount = String.format("%.2f", amount);
            String tag54 = "54" + String.format("%02d", formattedAmount.length()) + formattedAmount;

            String tag58 = "5802KH";

            String mName = merchantName.trim();
            String tag59 = "59" + String.format("%02d", mName.length()) + mName;

            String mCity = merchantCity.trim();
            String tag60 = "60" + String.format("%02d", mCity.length()) + mCity;

            String refSubTag01 = "01" + String.format("%02d", txId.length()) + txId;
            String tag62 = "62" + String.format("%02d", refSubTag01.length()) + refSubTag01;

            String payload = p00 + p01 + tag29 + tag52 + tag53 + tag54 + tag58 + tag59 + tag60 + tag62 + "6304";
            String crc = calculateCRC16(payload);
            String rawQrPayload = payload + crc;

            String md5Hash = DigestUtils.md5DigestAsHex(rawQrPayload.getBytes()).toUpperCase();

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("qrString", rawQrPayload);
            responseData.put("md5", md5Hash);
            responseData.put("transactionId", txId);
            responseData.put("amount", amount);

            System.out.println("[BAKONG PRODUCTION KHQR] " + rawQrPayload);
            return ResponseEntity.ok(responseData);

        } catch (Exception e) {
            System.err.println("[KHQR GENERATION ERROR] " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to compile production KHQR due to system error"));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Object> verifyKhqrPayment(@RequestBody Map<String, String> request) {
        try {
            String md5 = request.get("md5");
            String transactionId = request.get("transactionId");

            if (md5 == null || md5.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "md5 parameter is required"));
            }

            String url = "https://api-bakong.nbc.gov.kh/v1/check_transaction_by_md5";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(bakongToken);

            Map<String, String> body = new HashMap<>();
            body.put("md5", md5);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> result = response.getBody();

            if (result != null && "0".equals(String.valueOf(result.get("responseCode")))) {
                if (transactionId != null) {
                    paymentService.updatePaymentSuccess(transactionId, "KHQR_CONFIRMED");
                }
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}