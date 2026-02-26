package com.farmeazy.dto;

public class DashboardStatsDto {
    private Long totalFarms;
    private Long totalCrops;
    private Long totalIrrigations;
    private Long upcomingIrrigations;
    
    public DashboardStatsDto() {}
    
    public DashboardStatsDto(Long totalFarms, Long totalCrops, Long totalIrrigations, Long upcomingIrrigations) {
        this.totalFarms = totalFarms;
        this.totalCrops = totalCrops;
        this.totalIrrigations = totalIrrigations;
        this.upcomingIrrigations = upcomingIrrigations;
    }
    
    public Long getTotalFarms() { return totalFarms; }
    public void setTotalFarms(Long totalFarms) { this.totalFarms = totalFarms; }
    public Long getTotalCrops() { return totalCrops; }
    public void setTotalCrops(Long totalCrops) { this.totalCrops = totalCrops; }
    public Long getTotalIrrigations() { return totalIrrigations; }
    public void setTotalIrrigations(Long totalIrrigations) { this.totalIrrigations = totalIrrigations; }
    public Long getUpcomingIrrigations() { return upcomingIrrigations; }
    public void setUpcomingIrrigations(Long upcomingIrrigations) { this.upcomingIrrigations = upcomingIrrigations; }
}
