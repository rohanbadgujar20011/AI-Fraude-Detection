package com.paymentservice.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Objects;

public class TransactionRequest {

    @NotBlank
    private String userId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String merchant;

    @NotBlank
    private String deviceId;

    @NotBlank
    private String location;

    public TransactionRequest() {
    }

    public TransactionRequest(String userId, BigDecimal amount, String merchant, String deviceId, String location) {
        this.userId = userId;
        this.amount = amount;
        this.merchant = merchant;
        this.deviceId = deviceId;
        this.location = location;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionRequest that = (TransactionRequest) o;
        return Objects.equals(userId, that.userId) && Objects.equals(amount, that.amount) && Objects.equals(merchant, that.merchant) && Objects.equals(deviceId, that.deviceId) && Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, amount, merchant, deviceId, location);
    }

    @Override
    public String toString() {
        return "TransactionRequest{" +
                "userId='" + userId + '\'' +
                ", amount=" + amount +
                ", merchant='" + merchant + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}
