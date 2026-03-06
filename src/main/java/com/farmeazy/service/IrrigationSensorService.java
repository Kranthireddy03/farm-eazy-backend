package com.farmeazy.service;

import com.farmeazy.entity.*;
import com.farmeazy.entity.IrrigationSensorData.SensorType;
import com.farmeazy.entity.IrrigationAutomationRule.RuleAction;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * IRRIGATION SENSOR SERVICE
 * 
 * PURPOSE: Manages smart irrigation based on sensor data.
 * Receives readings from IoT sensors and triggers automation rules.
 * 
 * KEY FEATURES:
 * - Receives sensor readings from local gateway
 * - Detects anomalies in sensor data
 * - Evaluates automation rules against readings
 * - Triggers irrigation start/stop based on conditions
 * - Sends alerts for critical conditions
 * 
 * SENSOR TYPES SUPPORTED:
 * - SOIL_MOISTURE: Percentage (0-100%)
 * - TEMPERATURE: Celsius
 * - HUMIDITY: Percentage (0-100%)
 * - WATER_FLOW: Liters per minute
 * - RAIN_GAUGE: mm of rainfall
 * - PH_LEVEL: pH scale (0-14)
 * - NUTRIENT_LEVEL: PPM
 * 
 * AUTOMATION EXAMPLES:
 * - If soil moisture < 30%, start irrigation
 * - If temperature > 40°C, send alert
 * - If rain > 5mm, skip irrigation
 */
@Service
public class IrrigationSensorService {

    private static final Logger logger = LoggerFactory.getLogger(IrrigationSensorService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");

    @Autowired
    private IrrigationSensorDataRepository sensorDataRepository;

    @Autowired
    private IrrigationAutomationRuleRepository ruleRepository;

    @Autowired
    private IrrigationScheduleRepository scheduleRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private CropRepository cropRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    // ========== SENSOR DATA INGESTION ==========

    /**
     * Records a new sensor reading.
     * Validates data, checks for anomalies, and triggers automation rules.
     */
    @Transactional
    public IrrigationSensorData recordReading(Long farmId, String sensorId, 
            SensorType sensorType, BigDecimal value, BigDecimal batteryLevel, Integer signalStrength) {
        
        logger.debug("SENSOR_READING: farmId={}, sensorId={}, type={}, value={}",
                farmId, sensorId, sensorType, value);
        
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + farmId));
        
        IrrigationSensorData reading = new IrrigationSensorData();
        reading.setFarm(farm);
        reading.setSensorId(sensorId);
        reading.setSensorType(sensorType);
        reading.setReadingValue(value);
        reading.setReadingUnit(getUnitForType(sensorType));
        reading.setReadingTimestamp(LocalDateTime.now());
        reading.setBatteryLevel(batteryLevel);
        reading.setSignalStrength(signalStrength);
        
        // Check for anomalies
        if (isAnomaly(sensorType, value)) {
            reading.setIsAnomaly(true);
            reading.setAnomalyReason(getAnomalyReason(sensorType, value));
            logger.warn("SENSOR_ANOMALY: sensorId={}, type={}, value={}, reason={}",
                    sensorId, sensorType, value, reading.getAnomalyReason());
        }
        
        reading = sensorDataRepository.save(reading);
        
        logger.info("SENSOR_RECORDED: id={}, farmId={}, sensorId={}, type={}, value={}",
                reading.getId(), farmId, sensorId, sensorType, value);
        
        // Process automation rules (async in production)
        if (!reading.getIsAnomaly()) {
            processAutomationRules(farm, sensorType, value);
        }
        
        return reading;
    }

    /**
     * Records reading for a specific crop.
     */
    @Transactional
    public IrrigationSensorData recordCropReading(Long farmId, Long cropId, String sensorId,
            SensorType sensorType, BigDecimal value) {
        
        IrrigationSensorData reading = recordReading(farmId, sensorId, sensorType, value, null, null);
        
        if (cropId != null) {
            Crop crop = cropRepository.findById(cropId).orElse(null);
            if (crop != null) {
                reading.setCrop(crop);
                sensorDataRepository.save(reading);
            }
        }
        
        return reading;
    }

    // ========== AUTOMATION RULES ==========

    /**
     * Creates a new automation rule.
     */
    @Transactional
    public IrrigationAutomationRule createRule(Long farmId, Long cropId, String ruleName,
            SensorType sensorType, IrrigationAutomationRule.ConditionOperator operator,
            BigDecimal thresholdValue, BigDecimal thresholdMax, RuleAction action, Integer priority) {
        
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + farmId));
        
        Crop crop = cropId != null ? cropRepository.findById(cropId).orElse(null) : null;
        
        IrrigationAutomationRule rule = new IrrigationAutomationRule();
        rule.setFarm(farm);
        rule.setCrop(crop);
        rule.setRuleName(ruleName);
        rule.setSensorType(sensorType);
        rule.setConditionOperator(operator);
        rule.setThresholdValue(thresholdValue);
        rule.setThresholdValueMax(thresholdMax);
        rule.setAction(action);
        rule.setPriority(priority != null ? priority : 5);
        rule.setIsActive(true);
        
        rule = ruleRepository.save(rule);
        
        logger.info("AUTOMATION_RULE_CREATED: id={}, farmId={}, name={}, sensorType={}, action={}",
                rule.getId(), farmId, ruleName, sensorType, action);
        auditLogger.info("AUTOMATION_RULE: action=CREATE, ruleId={}, farmId={}", rule.getId(), farmId);
        
        return rule;
    }

    /**
     * Processes automation rules against a reading.
     */
    private void processAutomationRules(Farm farm, SensorType sensorType, BigDecimal value) {
        List<IrrigationAutomationRule> rules = ruleRepository.findActiveRulesForSensor(
                farm.getId(), sensorType);
        
        for (IrrigationAutomationRule rule : rules) {
            if (rule.evaluateCondition(value)) {
                executeRuleAction(farm, rule, value);
            }
        }
    }

    /**
     * Executes the action defined in the rule.
     */
    private void executeRuleAction(Farm farm, IrrigationAutomationRule rule, BigDecimal value) {
        rule.recordTrigger();
        ruleRepository.save(rule);
        
        logger.info("AUTOMATION_TRIGGERED: ruleId={}, ruleName={}, action={}, triggerValue={}",
                rule.getId(), rule.getRuleName(), rule.getAction(), value);
        
        switch (rule.getAction()) {
            case START_IRRIGATION -> startIrrigation(farm, rule);
            case STOP_IRRIGATION -> stopIrrigation(farm, rule);
            case SEND_ALERT -> sendAlert(farm, rule, value);
            case ADJUST_DURATION -> adjustIrrigationDuration(farm, rule, value);
        }
    }

    /**
     * Starts irrigation based on rule trigger.
     */
    private void startIrrigation(Farm farm, IrrigationAutomationRule rule) {
        logger.info("IRRIGATION_AUTO_START: farmId={}, ruleName={}", farm.getId(), rule.getRuleName());
        // Integration with irrigation controller would go here
        // For now, log the action
        auditLogger.info("IRRIGATION: action=AUTO_START, farmId={}, rule={}", 
                farm.getId(), rule.getRuleName());
    }

    /**
     * Stops irrigation based on rule trigger.
     */
    private void stopIrrigation(Farm farm, IrrigationAutomationRule rule) {
        logger.info("IRRIGATION_AUTO_STOP: farmId={}, ruleName={}", farm.getId(), rule.getRuleName());
        auditLogger.info("IRRIGATION: action=AUTO_STOP, farmId={}, rule={}", 
                farm.getId(), rule.getRuleName());
    }

    /**
     * Sends alert to farm owner.
     */
    private void sendAlert(Farm farm, IrrigationAutomationRule rule, BigDecimal value) {
        try {
            User user = farm.getUser();
            String subject = "FarmEazy Alert: " + rule.getRuleName();
            String body = String.format(
                    "Alert triggered for farm: %s\n\n" +
                    "Rule: %s\n" +
                    "Sensor Type: %s\n" +
                    "Current Value: %s %s\n" +
                    "Threshold: %s\n\n" +
                    "Please check your farm conditions.\n\n" +
                    "FarmEazy Automation",
                    farm.getFarmName(),
                    rule.getRuleName(),
                    rule.getSensorType(),
                    value, new IrrigationSensorData().getUnitLabel(),
                    rule.getThresholdValue()
            );
            
            emailService.sendEmail(user.getEmail(), subject, body);
            logger.info("IRRIGATION_ALERT_SENT: farmId={}, userId={}, rule={}", 
                    farm.getId(), user.getId(), rule.getRuleName());
            
        } catch (Exception e) {
            logger.error("IRRIGATION_ALERT_FAILED: farmId={}, error={}", farm.getId(), e.getMessage());
        }
    }

    /**
     * Adjusts irrigation duration based on conditions.
     */
    private void adjustIrrigationDuration(Farm farm, IrrigationAutomationRule rule, BigDecimal value) {
        logger.info("IRRIGATION_DURATION_ADJUST: farmId={}, ruleName={}, value={}",
                farm.getId(), rule.getRuleName(), value);
        // Duration adjustment logic would go here
    }

    // ========== QUERY METHODS ==========

    /**
     * Gets sensor data for a farm with pagination.
     */
    @Transactional(readOnly = true)
    public Page<IrrigationSensorData> getFarmSensorData(Long farmId, Pageable pageable) {
        return sensorDataRepository.findByFarmIdOrderByReadingTimestampDesc(farmId, pageable);
    }

    /**
     * Gets latest readings for each sensor type on a farm.
     */
    @Transactional(readOnly = true)
    public List<IrrigationSensorData> getLatestReadings(Long farmId, SensorType sensorType) {
        return sensorDataRepository.findLatestReadingsByFarmAndType(farmId, sensorType);
    }

    /**
     * Gets automation rules for a farm.
     */
    @Transactional(readOnly = true)
    public List<IrrigationAutomationRule> getFarmRules(Long farmId) {
        return ruleRepository.findByFarmIdOrderByPriorityAsc(farmId);
    }

    /**
     * Gets active rules for a farm.
     */
    @Transactional(readOnly = true)
    public List<IrrigationAutomationRule> getActiveRules(Long farmId) {
        return ruleRepository.findByFarmIdAndIsActiveTrueOrderByPriorityAsc(farmId);
    }

    /**
     * Toggles a rule's active state.
     * 
     * WHY: Users need to enable/disable rules without deleting them.
     */
    @Transactional
    public IrrigationAutomationRule toggleRule(Long ruleId, Boolean enabled) {
        IrrigationAutomationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + ruleId));
        
        rule.setIsActive(enabled);
        
        logger.info("RULE_TOGGLED: ruleId={}, ruleName={}, enabled={}",
                ruleId, rule.getRuleName(), enabled);
        
        return ruleRepository.save(rule);
    }

    /**
     * Deletes an automation rule.
     * 
     * WHY: Users need to remove rules they no longer need.
     */
    @Transactional
    public void deleteRule(Long ruleId) {
        IrrigationAutomationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + ruleId));
        
        logger.info("RULE_DELETED: ruleId={}, ruleName={}", ruleId, rule.getRuleName());
        
        ruleRepository.delete(rule);
    }

    /**
     * Calculates average reading for analytics.
     */
    @Transactional(readOnly = true)
    public BigDecimal getAverageReading(Long farmId, SensorType sensorType, 
            LocalDateTime start, LocalDateTime end) {
        return sensorDataRepository.calculateAverageReading(farmId, sensorType, start, end);
    }

    // ========== UTILITY METHODS ==========

    /**
     * Gets unit for sensor type.
     */
    private String getUnitForType(SensorType type) {
        return switch (type) {
            case SOIL_MOISTURE, HUMIDITY -> "%";
            case TEMPERATURE -> "°C";
            case WATER_FLOW -> "L/min";
            case RAIN_GAUGE -> "mm";
            case PH_LEVEL -> "pH";
            case NUTRIENT_LEVEL -> "ppm";
        };
    }

    /**
     * Checks if reading is anomalous.
     */
    private boolean isAnomaly(SensorType type, BigDecimal value) {
        return switch (type) {
            case SOIL_MOISTURE, HUMIDITY -> value.compareTo(BigDecimal.ZERO) < 0 || 
                    value.compareTo(new BigDecimal("100")) > 0;
            case TEMPERATURE -> value.compareTo(new BigDecimal("-50")) < 0 || 
                    value.compareTo(new BigDecimal("70")) > 0;
            case WATER_FLOW -> value.compareTo(BigDecimal.ZERO) < 0 || 
                    value.compareTo(new BigDecimal("1000")) > 0;
            case RAIN_GAUGE -> value.compareTo(BigDecimal.ZERO) < 0 || 
                    value.compareTo(new BigDecimal("500")) > 0;
            case PH_LEVEL -> value.compareTo(BigDecimal.ZERO) < 0 || 
                    value.compareTo(new BigDecimal("14")) > 0;
            case NUTRIENT_LEVEL -> value.compareTo(BigDecimal.ZERO) < 0 || 
                    value.compareTo(new BigDecimal("10000")) > 0;
        };
    }

    /**
     * Gets reason for anomaly.
     */
    private String getAnomalyReason(SensorType type, BigDecimal value) {
        return switch (type) {
            case SOIL_MOISTURE, HUMIDITY -> "Value out of range (0-100%)";
            case TEMPERATURE -> "Temperature out of expected range";
            case WATER_FLOW -> "Flow rate outside normal parameters";
            case RAIN_GAUGE -> "Rainfall reading outside expected range";
            case PH_LEVEL -> "pH outside valid range (0-14)";
            case NUTRIENT_LEVEL -> "Nutrient level outside expected range";
        };
    }
}
