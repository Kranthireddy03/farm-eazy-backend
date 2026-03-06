package com.farmeazy.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * IRRIGATION SENSOR DATA ENTITY
 * 
 * PURPOSE: Stores sensor readings from IoT devices for smart irrigation.
 * Enables data-driven irrigation decisions based on real-time conditions.
 * 
 * KEY FEATURES:
 * - Multiple sensor types (soil moisture, temperature, humidity, etc.)
 * - Real-time data collection with timestamps
 * - Battery and signal tracking for device health
 * - Anomaly detection for data quality
 * - Links to farm and crop for contextual analysis
 * 
 * SENSOR INTEGRATION:
 * - Sensors send data via local gateway
 * - API endpoint receives sensor readings
 * - Data stored and processed for automation rules
 */
@Entity
@Table(name = "irrigation_sensor_data")
public class IrrigationSensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id")
    private Crop crop;

    @Column(name = "sensor_id", nullable = false, length = 50)
    private String sensorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false)
    private SensorType sensorType;

    @Column(name = "reading_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal readingValue;

    @Column(name = "reading_unit", length = 20)
    private String readingUnit;

    @Column(name = "reading_timestamp", nullable = false)
    private LocalDateTime readingTimestamp;

    @Column(name = "battery_level", precision = 5, scale = 2)
    private BigDecimal batteryLevel;

    @Column(name = "signal_strength")
    private Integer signalStrength;

    @Column(name = "is_anomaly")
    private Boolean isAnomaly = false;

    @Column(name = "anomaly_reason", length = 255)
    private String anomalyReason;

    @Column(name = "processed")
    private Boolean processed = false;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // No-args constructor
    public IrrigationSensorData() {
    }

    // All-args constructor
    public IrrigationSensorData(Long id, Farm farm, Crop crop, String sensorId,
                                 SensorType sensorType, BigDecimal readingValue, String readingUnit,
                                 LocalDateTime readingTimestamp, BigDecimal batteryLevel,
                                 Integer signalStrength, Boolean isAnomaly, String anomalyReason,
                                 Boolean processed, LocalDateTime processedAt, LocalDateTime createdAt) {
        this.id = id;
        this.farm = farm;
        this.crop = crop;
        this.sensorId = sensorId;
        this.sensorType = sensorType;
        this.readingValue = readingValue;
        this.readingUnit = readingUnit;
        this.readingTimestamp = readingTimestamp;
        this.batteryLevel = batteryLevel;
        this.signalStrength = signalStrength;
        this.isAnomaly = isAnomaly;
        this.anomalyReason = anomalyReason;
        this.processed = processed;
        this.processedAt = processedAt;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Farm getFarm() {
        return farm;
    }

    public void setFarm(Farm farm) {
        this.farm = farm;
    }

    public Crop getCrop() {
        return crop;
    }

    public void setCrop(Crop crop) {
        this.crop = crop;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public SensorType getSensorType() {
        return sensorType;
    }

    public void setSensorType(SensorType sensorType) {
        this.sensorType = sensorType;
    }

    public BigDecimal getReadingValue() {
        return readingValue;
    }

    public void setReadingValue(BigDecimal readingValue) {
        this.readingValue = readingValue;
    }

    public String getReadingUnit() {
        return readingUnit;
    }

    public void setReadingUnit(String readingUnit) {
        this.readingUnit = readingUnit;
    }

    public LocalDateTime getReadingTimestamp() {
        return readingTimestamp;
    }

    public void setReadingTimestamp(LocalDateTime readingTimestamp) {
        this.readingTimestamp = readingTimestamp;
    }

    public BigDecimal getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(BigDecimal batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public Integer getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(Integer signalStrength) {
        this.signalStrength = signalStrength;
    }

    public Boolean getIsAnomaly() {
        return isAnomaly;
    }

    public void setIsAnomaly(Boolean isAnomaly) {
        this.isAnomaly = isAnomaly;
    }

    public String getAnomalyReason() {
        return anomalyReason;
    }

    public void setAnomalyReason(String anomalyReason) {
        this.anomalyReason = anomalyReason;
    }

    public Boolean getProcessed() {
        return processed;
    }

    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Sensor Type Enum
    public enum SensorType {
        SOIL_MOISTURE,
        TEMPERATURE,
        HUMIDITY,
        WATER_FLOW,
        RAIN_GAUGE,
        PH_LEVEL,
        NUTRIENT_LEVEL
    }

    /**
     * Gets the unit label for the sensor type.
     */
    public String getUnitLabel() {
        return switch (sensorType) {
            case SOIL_MOISTURE -> "%";
            case TEMPERATURE -> "°C";
            case HUMIDITY -> "%";
            case WATER_FLOW -> "L/min";
            case RAIN_GAUGE -> "mm";
            case PH_LEVEL -> "pH";
            case NUTRIENT_LEVEL -> "ppm";
        };
    }
}
