package com.farmeazy.service;

import com.farmeazy.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class CaptchaService {

    @Value("${captcha.enabled:false}")
    private boolean enabled;

    @Value("${captcha.secret-key:}")
    private String secretKey;

    @Value("${captcha.verify-url:https://www.google.com/recaptcha/api/siteverify}")
    private String verifyUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void validateCaptcha(String token) {
        if (!enabled) {
            return;
        }

        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("CAPTCHA validation is required");
        }

        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("CAPTCHA secret is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("secret", secretKey);
        requestBody.add("response", token);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> responseEntity = restTemplate.postForEntity(verifyUrl, request, Map.class);
        Map<String, Object> response = responseEntity.getBody();

        if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
            throw new UnauthorizedException("CAPTCHA validation failed");
        }
    }
}
