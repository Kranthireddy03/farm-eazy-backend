package com.farmeazy.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * IRRIGATION AUTOMATION RULE ENTITY
 * 
 * PURPOSE: Defines automation rules for smart irrigation.
 * Triggers actions based on sensor thresholds.
 * 
 * KEY FEATURES:
 * - Condition-based triggering (less than, greater than, between)
 * - Configurable actions (start/stop irrigation, send alerts)
 * - Priority-based rule execution
 * - Trigger tracking and history
 * 
 * EXAMPLE RULES:
 * - If soil moisture < 30%, start irrigation
 * - If temperature > 40°C, send alert to farmer
 * - If rain gauge > 5mm, stop scheduled irrigation
 */
@Entity
@Table(name = "irrigation_automation_rule")
public class IrrigationAutomationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id")
    private Crop crop;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false)
    private IrrigationSensorData.SensorType sensorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_operator", nullable = false)
    private ConditionOperator conditionOperator;

    @Column(name = "threshold_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal thresholdValue;

    @Column(name = "threshold_value_max", precision = 10, scale = 4)
    private BigDecimal thresholdValueMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private RuleAction action;

    @Column(name = "action_parameters", columnDefinition = "JSON")
    private String actionParameters;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "priority")
    private Integer priority = 5;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "trigger_count")
    private Integer triggerCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // No-args constructor
    public IrrigationAutomationRule() {
    }

    // All-args constructor
    public IrrigationAutomationRule(Long id, Farm farm, Crop crop, String ruleName,
                                     IrrigationSensorData.SensorType sensorType,
                                     ConditionOperator conditionOperator, BigDecimal thresholdValue,
                                     BigDecimal thresholdValueMax, RuleAction action,
                                     String actionParameters, Boolean isActive, Integer priority,
                                     LocalDateTime lastTriggeredAt, Integer triggerCount,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.farm = farm;
        this.crop = crop;
        this.ruleName = ruleName;
        this.sensorType = sensorType;
        this.conditionOperator = conditionOperator;
        this.thresholdValue = thresholdValue;
        this.thresholdValueMax = thresholdValueMax;
        this.action = action;
        this.actionParameters = actionParameters;
        this.isActive = isActive;
        this.priority = priority;
        this.lastTriggeredAt = lastTriggeredAt;
        this.triggerCount = triggerCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public IrrigationSensorData.SensorType getSensorType() {
        return sensorType;
    }

    public void setSensorType(IrrigationSensorData.SensorType sensorType) {
        this.sensorType = sensorType;
    }

    public ConditionOperator getConditionOperator() {
        return conditionOperator;
    }

    public void setConditionOperator(ConditionOperator conditionOperator) {
        this.conditionOperator = conditionOperator;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public BigDecimal getThresholdValueMax() {
        return thresholdValueMax;
    }

    public void setThresholdValueMax(BigDecimal thresholdValueMax) {
        this.thresholdValueMax = thresholdValueMax;
    }

    public RuleAction getAction() {
        return action;
    }

    public void setAction(RuleAction action) {
        this.action = action;
    }

    public String getActionParameters() {
        return actionParameters;
    }

    public void setActionParameters(String actionParameters) {
        this.actionParameters = actionParameters;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public LocalDateTime getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }

    public Integer getTriggerCount() {
        return triggerCount;
    }

    public void setTriggerCount(Integer triggerCount) {
        this.triggerCount = triggerCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Condition Operator Enum
    public enum ConditionOperator {
        LESS_THAN,
        GREATER_THAN,
        EQUALS,
        BETWEEN,
        NOT_EQUALS
    }

    // Rule Action Enum
    public enum RuleAction {
        START_IRRIGATION,
        STOP_IRRIGATION,
        SEND_ALERT,
        ADJUST_DURATION
    }

    /**
     * Evaluates if the given value satisfies this rule's condition.
     */
    public boolean evaluateCondition(BigDecimal value) {
        if (value == null) {
            return false;
        }

        return switch (conditionOperator) {
            case LESS_THAN -> value.compareTo(thresholdValue) < 0;
            case GREATER_THAN -> value.compareTo(thresholdValue) > 0;
            case EQUALS -> value.compareTo(thresholdValue) == 0;
            case NOT_EQUALS -> value.compareTo(thresholdValue) != 0;
            case BETWEEN -> thresholdValueMax != null &&
                    value.compareTo(thresholdValue) >= 0 &&
                    value.compareTo(thresholdValueMax) <= 0;
        };
    }

    /**
     * Records that this rule was triggered.
     */
    public void recordTrigger() {
        lastTriggeredAt = LocalDateTime.now();
        triggerCount++;
    }
}
