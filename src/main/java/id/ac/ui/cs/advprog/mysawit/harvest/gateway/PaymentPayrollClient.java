package id.ac.ui.cs.advprog.mysawit.harvest.gateway;

import id.ac.ui.cs.advprog.mysawit.harvest.model.Harvest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymentPayrollClient {

    private static final BigDecimal NET_MULTIPLIER = new BigDecimal("0.90");
    private static final Logger logger = LoggerFactory.getLogger(PaymentPayrollClient.class);

    private final RestTemplate restTemplate;
    private final String paymentBaseUrl;

    public PaymentPayrollClient(
            RestTemplate restTemplate,
            @Value("${payment.service.base-url:http://localhost:8084}") String paymentBaseUrl) {
        this.restTemplate = restTemplate;
        this.paymentBaseUrl = paymentBaseUrl.replaceAll("/+$", "");
    }

    @Async
    public void createHarvestPayroll(Harvest harvest, String authorizationHeader) {
        try {
            BigDecimal buruhWagePerKg = readWageSetting("buruhWagePerKg");
            BigDecimal amount = buruhWagePerKg
                    .multiply(harvest.getWeightKg())
                    .multiply(NET_MULTIPLIER)
                    .setScale(2, RoundingMode.HALF_UP);

            Map<String, Object> body = Map.of(
                    "buruhId", harvest.getBuruhId(),
                    "buruhName", harvest.getBuruhName(),
                    "amount", amount,
                    "harvestId", harvest.getId().toString(),
                    "description", "Harvest payout - " + harvest.getWeightKg()
                            + " kg @ Rp" + buruhWagePerKg + "/kg (after 10% fee)"
            );

            restTemplate.exchange(
                    paymentBaseUrl + "/api/payroll/harvest/create",
                    HttpMethod.POST,
                    new HttpEntity<>(body, authHeaders(authorizationHeader)),
                    Map.class
            );
        } catch (Exception ex) {
            logger.error("Failed to create harvest payroll for {}: {}",
                    harvest.getId(), ex.getMessage());
        }
    }

    private BigDecimal readWageSetting(String fieldName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(
                paymentBaseUrl + "/api/payment/wage-settings", Map.class);
        Object data = response == null ? null : response.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            throw new IllegalStateException("Invalid wage settings response");
        }
        Object value = dataMap.get(fieldName);
        if (value == null) {
            throw new IllegalStateException("Missing wage setting: " + fieldName);
        }
        return new BigDecimal(String.valueOf(value));
    }

    private HttpHeaders authHeaders(String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        return headers;
    }
}
