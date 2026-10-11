package com.frauddetection.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Audit record created whenever a transaction triggers one or more fraud rules
 * or an elevated ML score. Stored in Postgres and published to the "fraud-alerts"
 * topic.
 */
@Entity
@Table(name = "fraud_alerts")
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionId;

    private String userId;

    private BigDecimal amount;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> triggeredReasons;

    private double mlScore;

    private Instant flaggedAt;

    @Enumerated(EnumType.STRING)
    private Verdict verdict;

    public FraudAlert() {
    }

    public FraudAlert(Long id, String transactionId, String userId, BigDecimal amount, List<String> triggeredReasons, double mlScore, Instant flaggedAt, Verdict verdict) {
        this.id = id;
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.triggeredReasons = triggeredReasons;
        this.mlScore = mlScore;
        this.flaggedAt = flaggedAt;
        this.verdict = verdict;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public List<String> getTriggeredReasons() {
        return triggeredReasons;
    }

    public void setTriggeredReasons(List<String> triggeredReasons) {
        this.triggeredReasons = triggeredReasons;
    }

    public double getMlScore() {
        return mlScore;
    }

    public void setMlScore(double mlScore) {
        this.mlScore = mlScore;
    }

    public Instant getFlaggedAt() {
        return flaggedAt;
    }

    public void setFlaggedAt(Instant flaggedAt) {
        this.flaggedAt = flaggedAt;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FraudAlert that = (FraudAlert) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "FraudAlert{" +
                "id=" + id +
                ", transactionId='" + transactionId + '\'' +
                ", userId='" + userId + '\'' +
                ", amount=" + amount +
                ", triggeredReasons=" + triggeredReasons +
                ", mlScore=" + mlScore +
                ", flaggedAt=" + flaggedAt +
                ", verdict=" + verdict +
                '}';
    }

    public enum Verdict {
        BLOCKED, REVIEW, APPROVED
    }
}
