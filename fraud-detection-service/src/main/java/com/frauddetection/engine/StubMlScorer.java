package com.frauddetection.engine;

import com.frauddetection.model.TransactionEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Placeholder scorer so the pipeline is end-to-end runnable from day one.
 * Replace with a real call to a model-serving endpoint once you've trained
 * something (see /ml-training/README.md for the offline training plan).
 *
 * This stub uses two cheap, explainable signals so demo behavior isn't
 * random: odd-hour transactions and round-number amounts (both mild,
 * real fraud-literature signals) nudge the score up.
 */
@Component
public class StubMlScorer implements MlScorer {

    @Override
    public double score(TransactionEvent txn) {
        double score = 0.1; // baseline

        int hour = Instant.now().atZone(java.time.ZoneOffset.UTC).getHour();
        if (hour >= 1 && hour <= 4) {
            score += 0.3; // odd-hour transactions skew riskier
        }

        if (txn.getAmount().remainder(java.math.BigDecimal.valueOf(1000)).signum() == 0) {
            score += 0.15; // suspiciously round amounts
        }

        return Math.min(score, 1.0);
    }
}
