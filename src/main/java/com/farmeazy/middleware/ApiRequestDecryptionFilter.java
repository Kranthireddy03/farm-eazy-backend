package com.farmeazy.middleware;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmeazy.security.ApiPayloadCryptoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 14)
public class ApiRequestDecryptionFilter extends OncePerRequestFilter {

    private final ApiPayloadCryptoService cryptoService;
    private final ObjectMapper objectMapper;

    @Value("${security.api.encryption.enabled:true}")
    private boolean encryptionEnabled;

    @Value("${security.api.encryption.require-encrypted-request:true}")
    private boolean requireEncryptedRequest;

    public ApiRequestDecryptionFilter(ApiPayloadCryptoService cryptoService, ObjectMapper objectMapper) {
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!encryptionEnabled) {
            return true;
        }
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return true;
        }
        if (path.startsWith("/api/auth/")
                || path.startsWith("/api/otp/")
            || path.startsWith("/api/test-email/")
                || path.startsWith("/api/public/")
                || path.startsWith("/api/faq-question")
                || path.startsWith("/api/faq-questions")
                || path.startsWith("/api/faq/question")) {
            return true;
        }
        if (path.startsWith("/api/products/media/") || path.startsWith("/api/payment/webhook") || path.startsWith("/api/razorpay/webhook")) {
            return true;
        }

        if (!("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method))) {
            return true;
        }

        String contentType = request.getContentType();
        return contentType == null || !contentType.toLowerCase().contains("application/json");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        byte[] requestBody = request.getInputStream().readAllBytes();
        if (requestBody.length == 0) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawBody = new String(requestBody, StandardCharsets.UTF_8);
        JsonNode node;
        try {
            node = objectMapper.readTree(rawBody);
        } catch (Exception ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON body");
            return;
        }

        JsonNode payloadNode = node.get("payload");
        if (payloadNode == null || payloadNode.asText().isBlank()) {
            if (requireEncryptedRequest) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Encrypted payload is required");
                return;
            }
            filterChain.doFilter(new BodyOverrideRequestWrapper(request, requestBody), response);
            return;
        }

        String encryptedPayload = payloadNode.asText();
        String decryptedJson;
        try {
            decryptedJson = cryptoService.decrypt(encryptedPayload);
        } catch (Exception ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to decrypt request payload");
            return;
        }

        request.setAttribute("api.request.encrypted", encryptedPayload);
        request.setAttribute("api.request.decrypted", decryptedJson);

        byte[] decryptedBody = decryptedJson.getBytes(StandardCharsets.UTF_8);
        filterChain.doFilter(new BodyOverrideRequestWrapper(request, decryptedBody), response);
    }

    private static class BodyOverrideRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;

        BodyOverrideRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return inputStream.read();
                }

                @Override
                public boolean isFinished() {
                    return inputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
