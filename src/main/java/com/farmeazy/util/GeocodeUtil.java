package com.farmeazy.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for reverse geocoding coordinates to location names
 * Uses OpenStreetMap Nominatim API (free, no API key required)
 */
@Component
public class GeocodeUtil {
    private static final Logger logger = LoggerFactory.getLogger(GeocodeUtil.class);
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse";
    
    /**
     * Parses latitude and longitude from location header string
     * Expected format: "Lat XX.XXX, Lon YY.YYY"
     * 
     * @param locationHeader Location header string
     * @return Double array [latitude, longitude] or null if parsing fails
     */
    public static double[] parseCoordinates(String locationHeader) {
        if (locationHeader == null || locationHeader.isBlank()) {
            return null;
        }
        
        try {
            // Pattern: "Lat X.XXX, Lon Y.YYY"
            Pattern pattern = Pattern.compile("Lat\\s+([-\\d.]+),\\s+Lon\\s+([-\\d.]+)");
            Matcher matcher = pattern.matcher(locationHeader);
            
            if (matcher.find()) {
                double lat = Double.parseDouble(matcher.group(1));
                double lon = Double.parseDouble(matcher.group(2));
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            logger.debug("Failed to parse coordinates from: {}", locationHeader);
        }
        
        return null;
    }
    
    /**
     * Reverse geocode coordinates to city and country
     * Uses OpenStreetMap Nominatim API
     * 
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Location string in format "City, Country" or null if lookup fails
     */
    public String reverseGeocode(double latitude, double longitude) {
        try {
            String url = String.format("%s?format=json&lat=%.6f&lon=%.6f&zoom=10&addressdetails=1",
                    NOMINATIM_URL, latitude, longitude);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("User-Agent", "FarmEazy-API")
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                Map<String, Object> data = objectMapper.readValue(response.body(), Map.class);
                Map<String, Object> address = (Map<String, Object>) data.get("address");
                
                if (address != null) {
                    String city = (String) address.get("city");
                    if (city == null) {
                        city = (String) address.get("town");
                    }
                    if (city == null) {
                        city = (String) address.get("village");
                    }
                    
                    String country = (String) address.get("country");
                    
                    if (city != null && country != null) {
                        return city + ", " + country;
                    } else if (country != null) {
                        return country;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Reverse geocoding failed for lat={}, lon={}: {}", latitude, longitude, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Convert location header (with coordinates) to actual location name
     * 
     * @param locationHeader Location header like "Lat 12.984, Lon 77.749"
     * @return Location name like "Bangalore, India" or original header if conversion fails
     */
    public String convertCoordinatesToLocation(String locationHeader) {
        if (locationHeader == null || locationHeader.isBlank()) {
            return null;
        }
        
        double[] coords = parseCoordinates(locationHeader);
        if (coords != null) {
            String location = reverseGeocode(coords[0], coords[1]);
            if (location != null) {
                logger.debug("Converted coordinates to location: {} -> {}", locationHeader, location);
                return location;
            }
        }
        
        // Return original if conversion fails
        return locationHeader;
    }
}
