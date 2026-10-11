package com.frauddetection.consumer;

import com.frauddetection.engine.MlScorer;
import com.frauddetection.engine.RuleEngine;
import com.frauddetection.engine.RuleResult;
import com.frauddetection.model.FraudAlert;
import com.frauddetection.model.TransactionEvent;
import com.frauddetection.service.FraudAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Core 3-tier pipeline: consume raw transaction -> rule engine ->
 * ML scorer -> 3-tier verdict (BLOCKED / REVIEW / APPROVED) -> publish & audit.
 */
@Component
public class FraudConsumer {

    private static final Logger log = LoggerFactory.getLogger(FraudConsumer.class);

    private final RuleEngine ruleEngine;
    private final MlScorer mlScorer;
    private final FraudAlertRepository alertRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.approved}")
    private String approvedTopic;

    @Value("${kafka.topics.fraud-alerts}")
    private String fraudAlertsTopic;

    @Value("${fraud.ml.score-threshold}")
    private double mlScoreThreshold;

    public FraudConsumer(RuleEngine ruleEngine, MlScorer mlScorer, FraudAlertRepository alertRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.ruleEngine = ruleEngine;
        this.mlScorer = mlScorer;
        this.alertRepository = alertRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "${kafka.topics.transactions}", groupId = "fraud-detection-group")
    public void onTransaction(TransactionEvent txn) {
        log.info("Evaluating transaction {} for user {}", txn.getTransactionId(), txn.getUserId());

        RuleResult ruleResult = ruleEngine.evaluate(txn);
        double mlScore = mlScorer.score(txn);

        List<String> reasons = new ArrayList<>(ruleResult.getTriggeredReasons());
        boolean highMlScore = mlScore >= mlScoreThreshold; // >= 0.70
        boolean mediumMlScore = mlScore >= 0.30;

        if (highMlScore || mediumMlScore) {
            reasons.add("ML_RISK_SCORE_ELEVATED");
        }

        // Determine 3-Tier Verdict
        FraudAlert.Verdict verdict;

        boolean hasCriticalRule = reasons.contains("AMOUNT_EXCEEDS_ABSOLUTE_LIMIT") || reasons.contains("IMPOSSIBLE_TRAVEL_SPEED_EXCEEDED");

        if (hasCriticalRule || highMlScore) {
            verdict = FraudAlert.Verdict.BLOCKED;
        } else if (!reasons.isEmpty() || mediumMlScore) {
            verdict = FraudAlert.Verdict.REVIEW;
        } else {
            verdict = FraudAlert.Verdict.APPROVED;
        }

        if (verdict == FraudAlert.Verdict.APPROVED) {
            handleApproved(txn);
        } else {
            handleAlert(txn, reasons, mlScore, verdict);
        }
    }

    private void handleAlert(TransactionEvent txn, List<String> reasons, double mlScore, FraudAlert.Verdict verdict) {
        log.warn("FLAGGED transaction {} verdict={} reasons={} mlScore={}",
                txn.getTransactionId(), verdict, reasons, mlScore);

        FraudAlert alert = new FraudAlert(
                null,
                txn.getTransactionId(),
                txn.getUserId(),
                txn.getAmount(),
                reasons,
                mlScore,
                Instant.now(),
                verdict
        );
        alertRepository.save(alert);

        kafkaTemplate.send(fraudAlertsTopic, txn.getUserId(), alert);
    }

    private void handleApproved(TransactionEvent txn) {
        log.info("Transaction {} approved", txn.getTransactionId());
        kafkaTemplate.send(approvedTopic, txn.getUserId(), txn.getTransactionId());
    }
}
