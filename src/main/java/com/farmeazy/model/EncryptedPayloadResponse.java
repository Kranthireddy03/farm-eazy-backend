package com.farmeazy.model;

public class EncryptedPayloadResponse {
    private String payload;

    public EncryptedPayloadResponse() {
    }

    public EncryptedPayloadResponse(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
