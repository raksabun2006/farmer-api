package co.cambodia.farmer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "bakong")
@Data
public class BakongProperties {
    private String apiUrl = "https://api-bakong.nbc.gov.kh/v1/check_transaction_by_md5";
    private String token;
    private String accountId;
    private String merchantName;
    private String city;
    private int timeoutMs = 10000;

    private Api api = new Api();
    private Account account = new Account();
    private Merchant merchant = new Merchant();

    @Data
    public static class Api {
        private String token;
    }

    @Data
    public static class Account {
        private String id;
    }

    @Data
    public static class Merchant {
        private String name;
        private String city;
    }

    public String getToken() {
        if (token != null && !token.isBlank()) return token;
        if (api != null && api.getToken() != null && !api.getToken().isBlank()) return api.getToken();
        return null;
    }

    public String getAccountId() {
        if (accountId != null && !accountId.isBlank()) return accountId;
        if (account != null && account.getId() != null && !account.getId().isBlank()) return account.getId();
        return null;
    }

    public String getMerchantName() {
        if (merchantName != null && !merchantName.isBlank()) return merchantName;
        if (merchant != null && merchant.getName() != null && !merchant.getName().isBlank()) return merchant.getName();
        return "BUN RAKSA";
    }

    public String getCity() {
        if (city != null && !city.isBlank()) return city;
        if (merchant != null && merchant.getCity() != null && !merchant.getCity().isBlank()) return merchant.getCity();
        return "Phnom Penh";
    }
}
