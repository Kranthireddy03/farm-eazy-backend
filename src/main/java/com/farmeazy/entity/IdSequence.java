package com.farmeazy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * ID SEQUENCE ENTITY
 * 
 * PURPOSE: Manages 5-digit sequential IDs for various entities.
 * Provides unique, human-readable IDs for orders, users, service requests, etc.
 * 
 * KEY FEATURES:
 * - Generates sequential IDs starting from 10000
 * - Supports prefix for entity identification (USR, ORD, SRV)
 * - Thread-safe increment with database locking
 * - Configurable min/max values and cycling
 */
@Entity
@Table(name = "sequence_generator")
public class IdSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sequence_name", nullable = false, unique = true, length = 50)
    private String sequenceName;

    @Column(name = "current_value", nullable = false)
    private Long currentValue = 10000L;

    @Column(name = "prefix", length = 10)
    private String prefix;

    @Column(name = "min_value")
    private Long minValue = 10000L;

    @Column(name = "max_value")
    private Long maxValue = 99999L;

    @Column(name = "increment_by")
    private Integer incrementBy = 1;

    @Column(name = "is_cyclic")
    private Boolean isCyclic = false;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // No-args constructor
    public IdSequence() {
    }

    // All-args constructor
    public IdSequence(Long id, String sequenceName, Long currentValue, String prefix,
                              Long minValue, Long maxValue, Integer incrementBy, Boolean isCyclic,
                              LocalDateTime lastUpdated) {
        this.id = id;
        this.sequenceName = sequenceName;
        this.currentValue = currentValue;
        this.prefix = prefix;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.incrementBy = incrementBy;
        this.isCyclic = isCyclic;
        this.lastUpdated = lastUpdated;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    /**
     * Gets the next value and increments the sequence.
     * Should be called within a transaction with proper locking.
     */
    public String getNextDisplayId() {
        Long nextValue = currentValue;
        currentValue += incrementBy;
        
        // Handle cycling if enabled
        if (currentValue > maxValue) {
            if (isCyclic) {
                currentValue = minValue;
            } else {
                throw new IllegalStateException("Sequence " + sequenceName + " has reached maximum value");
            }
        }
        
        return prefix != null ? prefix + nextValue : String.valueOf(nextValue);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSequenceName() {
        return sequenceName;
    }

    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public Long getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Long currentValue) {
        this.currentValue = currentValue;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Long getMinValue() {
        return minValue;
    }

    public void setMinValue(Long minValue) {
        this.minValue = minValue;
    }

    public Long getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Long maxValue) {
        this.maxValue = maxValue;
    }

    public Integer getIncrementBy() {
        return incrementBy;
    }

    public void setIncrementBy(Integer incrementBy) {
        this.incrementBy = incrementBy;
    }

    public Boolean getIsCyclic() {
        return isCyclic;
    }

    public void setIsCyclic(Boolean isCyclic) {
        this.isCyclic = isCyclic;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
