package com.paymentservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentservice.model.Transaction;
import com.paymentservice.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Listens for the fraud-detection-service's verdict topics and updates the
 * transaction's final status. This is what closes the loop: payment-service
 * publishes -> fraud-detection-service scores -> payment-service marks final.
 */
@Service
public class VerdictListener {

    private static final Logger log = LoggerFactory.getLogger(VerdictListener.class);

    private final TransactionRepository repository;
    private final ObjectMapper objectMapper;

    public VerdictListener(TransactionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "approved", groupId = "payment-service-verdicts")
    public void onApproved(String transactionId) {
        if (transactionId != null) {
            String cleanId = transactionId.replace("\"", "").trim();
            updateStatus(cleanId, Transaction.TransactionStatus.APPROVED);
        }
    }

    @KafkaListener(topics = "fraud-alerts", groupId = "payment-service-verdicts")
    public void onFraudAlert(String alertJson) {
        log.warn("Fraud alert received, raw payload: {}", alertJson);
        try {
            JsonNode node = objectMapper.readTree(alertJson);
            if (node.has("transactionId")) {
                String transactionId = node.get("transactionId").asText();
                Transaction.TransactionStatus targetStatus = Transaction.TransactionStatus.BLOCKED;

                if (node.has("verdict")) {
                    String verdictStr = node.get("verdict").asText().toUpperCase();
                    if ("REVIEW".equals(verdictStr)) {
                        targetStatus = Transaction.TransactionStatus.REVIEW;
                    }
                }

                updateStatus(transactionId, targetStatus);
            }
        } catch (Exception e) {
            log.error("Failed to parse fraud alert JSON: {}", alertJson, e);
        }
    }

    private void updateStatus(String transactionId, Transaction.TransactionStatus status) {
        repository.findById(transactionId).ifPresentOrElse(
            txn -> {
                txn.setStatus(status);
                repository.save(txn);
                log.info("Transaction {} marked as {}", transactionId, status);
            },
            () -> log.warn("Transaction ID {} not found when trying to set status to {}", transactionId, status)
        );
    }
}
