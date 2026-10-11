package com.paymentservice.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single payment transaction submitted through the gateway.
 * Persisted for audit purposes AND published to Kafka for real-time
 * fraud evaluation by the fraud-detection-service.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    private String transactionId;

    private String userId;

    private BigDecimal amount;

    private String merchant;

    private String deviceId;

    private String location; // e.g. "IN-PUNE" or lat,long

    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    public Transaction() {
    }

    public Transaction(String transactionId, String userId, BigDecimal amount, String merchant, String deviceId, String location, Instant timestamp, TransactionStatus status) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.merchant = merchant;
        this.deviceId = deviceId;
        this.location = location;
        this.timestamp = timestamp;
        this.status = status;
    }

    public static Transaction newTransaction(String userId, BigDecimal amount,
                                              String merchant, String deviceId, String location) {
        Transaction txn = new Transaction();
        txn.setTransactionId(UUID.randomUUID().toString());
        txn.setUserId(userId);
        txn.setAmount(amount);
        txn.setMerchant(merchant);
        txn.setDeviceId(deviceId);
        txn.setLocation(location);
        txn.setTimestamp(Instant.now());
        txn.setStatus(TransactionStatus.PENDING);
        return txn;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", userId='" + userId + '\'' +
                ", amount=" + amount +
                ", merchant='" + merchant + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", location='" + location + '\'' +
                ", timestamp=" + timestamp +
                ", status=" + status +
                '}';
    }

    public enum TransactionStatus {
        PENDING, APPROVED, REVIEW, BLOCKED
    }
}
