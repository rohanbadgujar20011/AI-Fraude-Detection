package com.paymentservice.service;

import com.paymentservice.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.transactions}")
    private String transactionsTopic;

    public TransactionProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes the transaction to Kafka. Keyed by userId so all events
     * for a given user land on the same partition — this preserves per-user
     * ordering, which the fraud engine's velocity/rolling-window checks
     * depend on.
     */
    public void publish(Transaction txn) {
        kafkaTemplate.send(transactionsTopic, txn.getUserId(), txn);
        log.info("Published transaction {} for user {} to topic {}",
                txn.getTransactionId(), txn.getUserId(), transactionsTopic);
    }
}
