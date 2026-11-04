package com.companya.resort.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class CancelHoldRequest {

    @JsonAlias({"oneTimePaymentToken"})
    private String token;

    public CancelHoldRequest() {
    }

    public CancelHoldRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
