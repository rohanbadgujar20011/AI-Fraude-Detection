package com.frauddetection.engine;

import com.frauddetection.model.TransactionEvent;
import com.frauddetection.service.UserProfileService;
import com.frauddetection.util.HaversineUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Layer 1 fraud check — fast, deterministic, explainable rules. Runs on
 * every transaction before the ML layer.
 */
@Component
public class RuleEngine {

    private final UserProfileService profileService;

    @Value("${fraud.rules.amount-multiplier-threshold}")
    private double amountMultiplierThreshold;

    @Value("${fraud.rules.max-txns-per-minute}")
    private long maxTxnsPerMinute;

    @Value("${fraud.rules.high-amount-absolute}")
    private double highAmountAbsolute;

    private static final List<String> HIGH_RISK_KEYWORDS = Arrays.asList(
            "CRYPTO", "CASINO", "GAMBLING", "GIFT_CARD", "WIRE_TRANSFER", "JEWELRY"
    );

    public RuleEngine(UserProfileService profileService) {
        this.profileService = profileService;
    }

    public RuleResult evaluate(TransactionEvent txn) {
        RuleResult result = new RuleResult();
        String userId = txn.getUserId();

        checkAbsoluteAmount(txn, result);
        checkAmountSpike(txn, result, userId);
        checkVelocity(result, userId);
        checkImpossibleTravelSpeed(txn, result, userId);
        checkNewDevice(txn, result, userId);
        checkHighRiskMerchant(txn, result);

        // Update rolling state AFTER evaluation so this txn doesn't skew
        // the baseline used to judge itself.
        long now = txn.getTimestamp() != null ? txn.getTimestamp().getEpochSecond() : Instant.now().getEpochSecond();
        profileService.updateRollingAverage(userId, txn.getAmount());
        profileService.setLastLocation(userId, txn.getLocation(), now);
        profileService.setLastDevice(userId, txn.getDeviceId());

        return result;
    }

    private void checkAbsoluteAmount(TransactionEvent txn, RuleResult result) {
        if (txn.getAmount().doubleValue() > highAmountAbsolute) {
            result.flag("AMOUNT_EXCEEDS_ABSOLUTE_LIMIT");
        }
    }

    private void checkAmountSpike(TransactionEvent txn, RuleResult result, String userId) {
        BigDecimal avg = profileService.getRollingAverage(userId);
        if (avg != null && avg.doubleValue() > 0) {
            double ratio = txn.getAmount().doubleValue() / avg.doubleValue();
            if (ratio > amountMultiplierThreshold) {
                result.flag("AMOUNT_SPIKE_VS_USER_AVERAGE");
            }
        }
    }

    private void checkVelocity(RuleResult result, String userId) {
        long count = profileService.incrementAndGetVelocity(userId);
        if (count > maxTxnsPerMinute) {
            result.flag("VELOCITY_LIMIT_EXCEEDED");
        }
    }

    private void checkImpossibleTravelSpeed(TransactionEvent txn, RuleResult result, String userId) {
        String lastLocation = profileService.getLastLocation(userId);
        Long lastTime = profileService.getLastLocationTime(userId);
        long currentTime = txn.getTimestamp() != null ? txn.getTimestamp().getEpochSecond() : Instant.now().getEpochSecond();

        if (lastLocation != null && lastTime != null && !lastLocation.equalsIgnoreCase(txn.getLocation())) {
            double[] prevCoords = HaversineUtil.parseCoordinates(lastLocation);
            double[] currCoords = HaversineUtil.parseCoordinates(txn.getLocation());

            if (prevCoords != null && currCoords != null) {
                double distanceKm = HaversineUtil.calculateDistanceKm(prevCoords[0], prevCoords[1], currCoords[0], currCoords[1]);
                long secondsElapsed = currentTime - lastTime;
                double speedKmH = HaversineUtil.calculateSpeedKmH(distanceKm, secondsElapsed);

                // Commercial aircraft speed limit is ~800 km/h
                if (speedKmH > 800.0 && distanceKm > 50.0) {
                    result.flag("IMPOSSIBLE_TRAVEL_SPEED_EXCEEDED");
                } else if (distanceKm > 10.0 && secondsElapsed < 300) {
                    result.flag("LOCATION_CHANGED_WITHIN_WINDOW");
                }
            } else {
                result.flag("LOCATION_CHANGED_WITHIN_WINDOW");
            }
        }
    }

    private void checkNewDevice(TransactionEvent txn, RuleResult result, String userId) {
        String lastDevice = profileService.getLastDevice(userId);
        boolean isNewDevice = lastDevice != null && !lastDevice.equals(txn.getDeviceId());
        boolean isHighValue = txn.getAmount().doubleValue() > highAmountAbsolute * 0.5;
        if (isNewDevice && isHighValue) {
            result.flag("NEW_DEVICE_HIGH_VALUE");
        }
    }

    private void checkHighRiskMerchant(TransactionEvent txn, RuleResult result) {
        if (txn.getMerchant() != null) {
            String upperMerchant = txn.getMerchant().toUpperCase();
            boolean isHighRiskCategory = HIGH_RISK_KEYWORDS.stream().anyMatch(upperMerchant::contains);
            if (isHighRiskCategory && txn.getAmount().doubleValue() > 1000.0) {
                result.flag("HIGH_RISK_MERCHANT_CATEGORY");
            }
        }
    }
}
