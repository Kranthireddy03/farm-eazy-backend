package com.farmeazy.dto;

/**
 * Generic API Response wrapper for all REST endpoints
 * Ensures consistent response format across the application
 */
public class ApiResponse<T> {
    private String message;
    private T data;
    private Object error;

    public ApiResponse() {}

    public ApiResponse(String message, T data, Object error) {
        this.message = message;
        this.data = data;
        this.error = error;
    }

    // Getters & Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }
}
