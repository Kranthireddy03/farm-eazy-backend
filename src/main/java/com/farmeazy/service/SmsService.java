package com.farmeazy.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SmsService {

    @Value("${msg91.authKey}")
    private String authKey;

    @Value("${msg91.senderId}")
    private String senderId;

    @Value("${msg91.templateId}")
    private String templateId;

    private static final String MSG91_URL = "https://api.msg91.com/api/v5/flow/";

    public void sendSms(String phoneNumber, String otp) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            System.err.println("[WARN] Phone number missing — skipping SMS");
            return;
        }
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authkey", authKey);

        // Always format as 91XXXXXXXXXX (no plus)
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (!digits.startsWith("91")) {
            digits = "91" + digits;
        }
        String fullPhone = digits;

        String body = String.format("{" +
                "\"flow_id\":\"%s\"," +
                "\"sender\":\"%s\"," +
                "\"mobiles\":\"%s\"," +
                "\"OTP\":\"%s\"}",
                templateId, senderId, fullPhone, otp);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        int attempts = 0;
        boolean sent = false;
        Exception lastException = null;
        while (!sent && attempts < 2) {
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(MSG91_URL, entity, String.class);
                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new RuntimeException("Failed to send SMS: " + response.getBody());
                }
                sent = true;
            } catch (Exception e) {
                attempts++;
                lastException = e;
            }
        }
        if (!sent) {
            throw new RuntimeException("Communication failed: SMS could not be sent. Please retry.", lastException);
        }
    }
}
