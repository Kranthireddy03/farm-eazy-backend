package com.farmeazy.security;

import com.farmeazy.model.EncryptedPayloadResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice(annotations = {RestController.class, Controller.class})
public class ApiEncryptionResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ApiPayloadCryptoService cryptoService;
    private final ObjectMapper objectMapper;

    @Value("${security.api.encryption.enabled:true}")
    private boolean encryptionEnabled;

    public ApiEncryptionResponseAdvice(ApiPayloadCryptoService cryptoService, ObjectMapper objectMapper) {
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Skip encryption wrapping for plain String responses to avoid StringHttpMessageConverter class cast issues.
        return !StringHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (!encryptionEnabled) {
            return body;
        }

        String path = request.getURI().getPath();
        if (path == null || !path.startsWith("/api/")) {
            return body;
        }
        if (path.startsWith("/api/auth/")
                || path.startsWith("/api/otp/")
            || path.startsWith("/api/test-email/")
                || path.startsWith("/api/public/")
                || path.startsWith("/api/faq-question/")
                || path.startsWith("/api/faq/question")
                || path.startsWith("/api/admin/faq-questions/stream")
                || path.startsWith("/api/products/media/")
                || path.startsWith("/api/payment/webhook")
                || path.startsWith("/api/razorpay/webhook")) {
            return body;
        }
        if (selectedContentType != null && MediaType.TEXT_EVENT_STREAM.includes(selectedContentType)) {
            return body;
        }
        if (selectedContentType != null && !MediaType.APPLICATION_JSON.includes(selectedContentType)
                && !selectedContentType.getSubtype().toLowerCase().contains("json")) {
            return body;
        }
        if (body instanceof EncryptedPayloadResponse) {
            return body;
        }

        try {
            String rawJson = objectMapper.writeValueAsString(body);
            String encryptedPayload = cryptoService.encrypt(rawJson);

            if (request instanceof ServletServerHttpRequest servletRequest) {
                servletRequest.getServletRequest().setAttribute("api.response.plain", rawJson);
                servletRequest.getServletRequest().setAttribute("api.response.encrypted", encryptedPayload);
            }

            return new EncryptedPayloadResponse(encryptedPayload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize response payload", ex);
        }
    }
}
