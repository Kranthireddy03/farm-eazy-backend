package com.farmeazy.dto;

import jakarta.validation.constraints.NotBlank;

public class GoogleSignInRequestDto {

    @NotBlank(message = "Google credential is required")
    private String credential;

    public GoogleSignInRequestDto() {
    }

    public GoogleSignInRequestDto(String credential) {
        this.credential = credential;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }
}