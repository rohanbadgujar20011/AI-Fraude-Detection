package com.frauddetection.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.model.TransactionEvent;
import com.frauddetection.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneOffset;

@Component
@Primary
public class RestMlScorer implements MlScorer {

    private static final Logger log = LoggerFactory.getLogger(RestMlScorer.class);

    private final UserProfileService profileService;
    private final StubMlScorer fallbackScorer;
    private final ObjectMapper objectMapper;

    @Value("${fraud.ml.service-url:http://localhost:5000/score}")
    private String mlServiceUrl;

    public RestMlScorer(UserProfileService profileService, StubMlScorer fallbackScorer) {
        this.profileService = profileService;
        this.fallbackScorer = fallbackScorer;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public double score(TransactionEvent txn) {
        try {
            int hour = txn.getTimestamp() != null
                    ? txn.getTimestamp().atZone(ZoneOffset.UTC).getHour()
                    : Instant.now().atZone(ZoneOffset.UTC).getHour();

            BigDecimal avg = profileService.getRollingAverage(txn.getUserId());
            double ratio = (avg != null && avg.doubleValue() > 0)
                    ? txn.getAmount().doubleValue() / avg.doubleValue()
                    : 1.0;

            String merchant = txn.getMerchant() != null ? txn.getMerchant().toUpperCase() : "";
            int isHighRiskMerchant = (merchant.contains("CRYPTO") || merchant.contains("CASINO")
                    || merchant.contains("GAMBLING") || merchant.contains("GIFT_CARD")) ? 1 : 0;

            String lastDevice = profileService.getLastDevice(txn.getUserId());
            int isNewDevice = (lastDevice != null && !lastDevice.equals(txn.getDeviceId())) ? 1 : 0;

            String jsonPayload = """
                {
                    "amount": %f,
                    "hour": %d,
                    "ratio": %f,
                    "isHighRiskMerchant": %d,
                    "isNewDevice": %d
                }
                """.formatted(txn.getAmount().doubleValue(), hour, ratio, isHighRiskMerchant, isNewDevice);

            URL url = new URL(mlServiceUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes());
                os.flush();
            }

            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    JsonNode resp = objectMapper.readTree(is);
                    if (resp.has("fraudProbability")) {
                        double prob = resp.get("fraudProbability").asDouble();
                        log.info("REST ML Scorer returned probability {} for txn {}", prob, txn.getTransactionId());
                        return prob;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("REST ML service unreachable ({}), using fallback heuristic scorer", e.getMessage());
        }

        return fallbackScorer.score(txn);
    }
}
