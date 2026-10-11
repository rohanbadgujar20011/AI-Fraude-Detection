package com.frauddetection.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Inbound event consumed from Kafka topic "transactions". Identical field structure
 * to payment-service's Transaction entity, but kept as a distinct DTO here so the
 * two services remain loosely coupled.
 */
public class TransactionEvent {

    private String transactionId;
    private String userId;
    private BigDecimal amount;
    private String merchant;
    private String deviceId;
    private String location;
    private Instant timestamp;

    public TransactionEvent() {
    }

    public TransactionEvent(String transactionId, String userId, BigDecimal amount, String merchant, String deviceId, String location, Instant timestamp) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.merchant = merchant;
        this.deviceId = deviceId;
        this.location = location;
        this.timestamp = timestamp;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionEvent that = (TransactionEvent) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "TransactionEvent{" +
                "transactionId='" + transactionId + '\'' +
                ", userId='" + userId + '\'' +
                ", amount=" + amount +
                ", merchant='" + merchant + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", location='" + location + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
