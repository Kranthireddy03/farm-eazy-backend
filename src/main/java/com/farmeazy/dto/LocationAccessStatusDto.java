package com.farmeazy.dto;

public class LocationAccessStatusDto {

    private boolean allowed;
    private String message;
    private Long matchedLocationId;
    private String matchedLocationName;

    public LocationAccessStatusDto() {
    }

    public LocationAccessStatusDto(boolean allowed, String message, Long matchedLocationId, String matchedLocationName) {
        this.allowed = allowed;
        this.message = message;
        this.matchedLocationId = matchedLocationId;
        this.matchedLocationName = matchedLocationName;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getMatchedLocationId() {
        return matchedLocationId;
    }

    public void setMatchedLocationId(Long matchedLocationId) {
        this.matchedLocationId = matchedLocationId;
    }

    public String getMatchedLocationName() {
        return matchedLocationName;
    }

    public void setMatchedLocationName(String matchedLocationName) {
        this.matchedLocationName = matchedLocationName;
    }
}
