package com.companya.resort.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class PaymentWebhookRequest {

    @JsonAlias({"token"})
    private String oneTimePaymentToken;

    private String status;

    private String mockGatewayRef;

    private String paymentMethod;

    public String getEffectiveToken() {
        return oneTimePaymentToken;
    }

    public PaymentWebhookRequest() {
    }

    public PaymentWebhookRequest(String oneTimePaymentToken, String status) {
        this.oneTimePaymentToken = oneTimePaymentToken;
        this.status = status;
    }

    public PaymentWebhookRequest(String oneTimePaymentToken, String status, String mockGatewayRef, String paymentMethod) {
        this.oneTimePaymentToken = oneTimePaymentToken;
        this.status = status;
        this.mockGatewayRef = mockGatewayRef;
        this.paymentMethod = paymentMethod;
    }

    public String getOneTimePaymentToken() {
        return oneTimePaymentToken;
    }

    public void setOneTimePaymentToken(String oneTimePaymentToken) {
        this.oneTimePaymentToken = oneTimePaymentToken;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMockGatewayRef() {
        return mockGatewayRef;
    }

    public void setMockGatewayRef(String mockGatewayRef) {
        this.mockGatewayRef = mockGatewayRef;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
