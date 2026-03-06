package com.farmeazy.controller;

import com.farmeazy.entity.IrrigationAutomationRule;
import com.farmeazy.entity.IrrigationSensorData;
import com.farmeazy.entity.IrrigationSensorData.SensorType;
import com.farmeazy.entity.IrrigationAutomationRule.ConditionOperator;
import com.farmeazy.entity.IrrigationAutomationRule.RuleAction;
import com.farmeazy.entity.User;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.service.IrrigationSensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * IRRIGATION SENSOR CONTROLLER
 * 
 * PURPOSE: REST API for smart irrigation sensor management.
 * Receives data from IoT sensors and manages automation rules.
 * 
 * ENDPOINTS:
 * - POST /api/irrigation-sensors/readings         - Submit new sensor reading
 * - GET /api/irrigation-sensors/farm/{farmId}/data    - Get all sensor data
 * - GET /api/irrigation-sensors/farm/{farmId}/latest  - Get latest readings
 * - POST /api/irrigation-sensors/farm/{farmId}/rules   - Create automation rule
 * - GET /api/irrigation-sensors/farm/{farmId}/rules   - Get farm rules
 * - PATCH /api/irrigation-sensors/rules/{id}           - Update rule (enable/disable)
 * - DELETE /api/irrigation-sensors/rules/{id}          - Delete rule
 * 
 * WHY THIS API EXISTS:
 * Smart irrigation requires real-time sensor data from the field.
 * Sensors measure soil moisture, temperature, humidity, etc.
 * This data drives automated irrigation decisions.
 * 
 * SENSOR INTEGRATION:
 * Local sensors connect to a gateway device that POSTs readings to this API.
 * The API validates data, detects anomalies, and triggers automation rules.
 */
@RestController
@RequestMapping("/api/irrigation-sensors")
@Tag(name = "Irrigation Sensors", description = "APIs for IoT sensor data and smart irrigation automation")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class IrrigationSensorController {

    private static final Logger logger = LoggerFactory.getLogger(IrrigationSensorController.class);

    @Autowired
    private IrrigationSensorService sensorService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Submits a new sensor reading.
     * 
     * WHY: IoT sensors in the field periodically send their measurements
     * to this endpoint. The data is stored and processed for automation.
     * 
     * @param body Sensor reading data
     * @return Confirmation of recorded reading
     */
    @PostMapping("/readings")
    @Operation(summary = "Submit sensor reading",
               description = "Submit a new reading from an IoT sensor in the field")
    public ResponseEntity<?> submitReading(@RequestBody Map<String, Object> body) {
        
        Long farmId = Long.valueOf(body.get("farmId").toString());
        String sensorId = body.get("sensorId").toString();
        String sensorTypeStr = body.get("sensorType").toString();
        BigDecimal value = new BigDecimal(body.get("value").toString());
        BigDecimal batteryLevel = body.containsKey("batteryLevel") 
                ? new BigDecimal(body.get("batteryLevel").toString()) 
                : null;
        Integer signalStrength = body.containsKey("signalStrength") 
                ? Integer.valueOf(body.get("signalStrength").toString()) 
                : null;
        
        logger.info("SENSOR_API: farmId={}, sensorId={}, type={}, value={}",
                farmId, sensorId, sensorTypeStr, value);
        
        SensorType sensorType = SensorType.valueOf(sensorTypeStr);
        
        IrrigationSensorData reading = sensorService.recordReading(
                farmId, sensorId, sensorType, value, batteryLevel, signalStrength);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "readingId", reading.getId(),
                "sensorId", sensorId,
                "value", value,
                "unit", reading.getReadingUnit(),
                "isAnomaly", reading.getIsAnomaly(),
                "timestamp", reading.getReadingTimestamp()
        ));
    }

    /**
     * Gets sensor data for a farm.
     * 
     * WHY: Dashboard needs to display sensor readings and trends.
     */
    @GetMapping("/farm/{farmId}/data")
    @Operation(summary = "Get farm sensor data",
               description = "Get paginated sensor readings for a farm")
    public ResponseEntity<?> getFarmSensorData(
            @PathVariable Long farmId,
            @RequestParam(required = false) String sensorType,
            Pageable pageable) {
        
        Page<IrrigationSensorData> data = sensorService.getFarmSensorData(farmId, pageable);
        
        Page<?> response = data.map(reading -> Map.of(
                "id", reading.getId(),
                "sensorId", reading.getSensorId(),
                "sensorType", reading.getSensorType().name(),
                "value", reading.getReadingValue(),
                "unit", reading.getReadingUnit(),
                "timestamp", reading.getReadingTimestamp(),
                "isAnomaly", reading.getIsAnomaly(),
                "batteryLevel", reading.getBatteryLevel(),
                "signalStrength", reading.getSignalStrength()
        ));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets latest readings for each sensor type.
     * 
     * WHY: Quick view of current farm conditions.
     */
    @GetMapping("/farm/{farmId}/latest")
    @Operation(summary = "Get latest readings",
               description = "Get the most recent reading for each sensor type on a farm")
    public ResponseEntity<?> getLatestReadings(
            @PathVariable Long farmId,
            @RequestParam String sensorType) {
        
        SensorType type = SensorType.valueOf(sensorType);
        List<IrrigationSensorData> readings = sensorService.getLatestReadings(farmId, type);
        
        List<?> response = readings.stream()
                .map(r -> Map.of(
                        "sensorId", r.getSensorId(),
                        "sensorType", r.getSensorType().name(),
                        "value", r.getReadingValue(),
                        "unit", r.getReadingUnit(),
                        "timestamp", r.getReadingTimestamp()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new automation rule.
     * 
     * WHY: Users define rules to automate irrigation based on sensor readings.
     * For example: "If soil moisture drops below 30%, start irrigation."
     */
    @PostMapping("/farm/{farmId}/rules")
    @Operation(summary = "Create automation rule",
               description = "Create a new automation rule for sensor-based irrigation control")
    public ResponseEntity<?> createRule(
            @PathVariable Long farmId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        
        Long cropId = body.containsKey("cropId") ? Long.valueOf(body.get("cropId").toString()) : null;
        String ruleName = body.get("ruleName").toString();
        SensorType sensorType = SensorType.valueOf(body.get("sensorType").toString());
        ConditionOperator operator = ConditionOperator.valueOf(body.get("operator").toString());
        BigDecimal threshold = new BigDecimal(body.get("threshold").toString());
        BigDecimal thresholdMax = body.containsKey("thresholdMax") 
                ? new BigDecimal(body.get("thresholdMax").toString()) 
                : null;
        RuleAction action = RuleAction.valueOf(body.get("action").toString());
        Integer priority = body.containsKey("priority") 
                ? Integer.valueOf(body.get("priority").toString()) 
                : 5;
        
        logger.info("RULE_CREATE_API: farmId={}, ruleName={}, sensorType={}, action={}",
                farmId, ruleName, sensorType, action);
        
        IrrigationAutomationRule rule = sensorService.createRule(
                farmId, cropId, ruleName, sensorType, operator, threshold, thresholdMax, action, priority);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "ruleId", rule.getId(),
                "ruleName", rule.getRuleName(),
                "message", "Automation rule created successfully"
        ));
    }

    /**
     * Gets automation rules for a farm.
     */
    @GetMapping("/farm/{farmId}/rules")
    @Operation(summary = "Get farm rules",
               description = "Get all automation rules for a farm")
    public ResponseEntity<?> getFarmRules(@PathVariable Long farmId) {
        
        List<IrrigationAutomationRule> rules = sensorService.getFarmRules(farmId);
        
        List<?> response = rules.stream()
                .map(rule -> {
                    Map<String, Object> ruleMap = new LinkedHashMap<>();
                    ruleMap.put("id", rule.getId());
                    ruleMap.put("ruleName", rule.getRuleName());
                    ruleMap.put("sensorType", rule.getSensorType().name());
                    ruleMap.put("operator", rule.getConditionOperator().name());
                    ruleMap.put("threshold", rule.getThresholdValue());
                    ruleMap.put("thresholdMax", rule.getThresholdValueMax());
                    ruleMap.put("action", rule.getAction().name());
                    ruleMap.put("isActive", rule.getIsActive());
                    ruleMap.put("priority", rule.getPriority());
                    ruleMap.put("triggerCount", rule.getTriggerCount());
                    ruleMap.put("lastTriggered", rule.getLastTriggeredAt());
                    return ruleMap;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets sensor type reference data.
     * 
     * WHY: Frontend needs to display dropdown options for sensor types.
     */
    @GetMapping("/types")
    @Operation(summary = "Get sensor types",
               description = "Get reference data for all supported sensor types")
    public ResponseEntity<?> getSensorTypes() {
        
        List<?> types = List.of(
                Map.of("type", "SOIL_MOISTURE", "label", "Soil Moisture", "unit", "%", 
                       "description", "Measures soil water content"),
                Map.of("type", "TEMPERATURE", "label", "Temperature", "unit", "°C",
                       "description", "Measures ambient temperature"),
                Map.of("type", "HUMIDITY", "label", "Humidity", "unit", "%",
                       "description", "Measures air humidity"),
                Map.of("type", "WATER_FLOW", "label", "Water Flow", "unit", "L/min",
                       "description", "Measures water flow rate"),
                Map.of("type", "RAIN_GAUGE", "label", "Rain Gauge", "unit", "mm",
                       "description", "Measures rainfall"),
                Map.of("type", "PH_LEVEL", "label", "pH Level", "unit", "pH",
                       "description", "Measures soil acidity/alkalinity"),
                Map.of("type", "NUTRIENT_LEVEL", "label", "Nutrient Level", "unit", "ppm",
                       "description", "Measures soil nutrient concentration")
        );
        
        return ResponseEntity.ok(types);
    }

    /**
     * Updates an automation rule (enable/disable).
     * 
     * WHY: Users need to toggle rules on/off without deleting them.
     */
    @PatchMapping("/rules/{ruleId}")
    @Operation(summary = "Update rule",
               description = "Update an automation rule's enabled state")
    public ResponseEntity<?> updateRule(
            @PathVariable Long ruleId,
            @RequestBody Map<String, Object> body) {
        
        Boolean enabled = (Boolean) body.get("enabled");
        
        logger.info("RULE_UPDATE_API: ruleId={}, enabled={}", ruleId, enabled);
        
        IrrigationAutomationRule rule = sensorService.toggleRule(ruleId, enabled);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "ruleId", rule.getId(),
                "ruleName", rule.getRuleName(),
                "isActive", rule.getIsActive(),
                "message", enabled ? "Rule enabled" : "Rule disabled"
        ));
    }

    /**
     * Deletes an automation rule.
     * 
     * WHY: Users need to remove rules they no longer need.
     */
    @DeleteMapping("/rules/{ruleId}")
    @Operation(summary = "Delete rule",
               description = "Delete an automation rule")
    public ResponseEntity<?> deleteRule(@PathVariable Long ruleId) {
        
        logger.info("RULE_DELETE_API: ruleId={}", ruleId);
        
        sensorService.deleteRule(ruleId);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "ruleId", ruleId,
                "message", "Rule deleted successfully"
        ));
    }
}
