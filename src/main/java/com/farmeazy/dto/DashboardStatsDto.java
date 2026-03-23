package com.farmeazy.dto;

public class DashboardStatsDto {
    private int totalTickets;
    private int pendingTickets;
    private int resolvedToday;
    private int pendingFaqs;
    private int totalTicketsTrend;
    private int pendingTicketsTrend;
    private int resolvedTodayTrend;
    private int pendingFaqsTrend;

    public DashboardStatsDto() {}

    public DashboardStatsDto(int totalTickets, int pendingTickets, int resolvedToday, int pendingFaqs,
                             int totalTicketsTrend, int pendingTicketsTrend, int resolvedTodayTrend, int pendingFaqsTrend) {
        this.totalTickets = totalTickets;
        this.pendingTickets = pendingTickets;
        this.resolvedToday = resolvedToday;
        this.pendingFaqs = pendingFaqs;
        this.totalTicketsTrend = totalTicketsTrend;
        this.pendingTicketsTrend = pendingTicketsTrend;
        this.resolvedTodayTrend = resolvedTodayTrend;
        this.pendingFaqsTrend = pendingFaqsTrend;
    }

    public DashboardStatsDto(int totalTickets, int pendingTickets, int resolvedToday, int pendingFaqs) {
        this(totalTickets, pendingTickets, resolvedToday, pendingFaqs, 0, 0, 0, 0);
    }

    public int getTotalTickets() { return totalTickets; }
    public void setTotalTickets(int totalTickets) { this.totalTickets = totalTickets; }
    public int getPendingTickets() { return pendingTickets; }
    public void setPendingTickets(int pendingTickets) { this.pendingTickets = pendingTickets; }
    public int getResolvedToday() { return resolvedToday; }
    public void setResolvedToday(int resolvedToday) { this.resolvedToday = resolvedToday; }
    public int getPendingFaqs() { return pendingFaqs; }
    public void setPendingFaqs(int pendingFaqs) { this.pendingFaqs = pendingFaqs; }

    public int getTotalTicketsTrend() { return totalTicketsTrend; }
    public void setTotalTicketsTrend(int totalTicketsTrend) { this.totalTicketsTrend = totalTicketsTrend; }
    public int getPendingTicketsTrend() { return pendingTicketsTrend; }
    public void setPendingTicketsTrend(int pendingTicketsTrend) { this.pendingTicketsTrend = pendingTicketsTrend; }
    public int getResolvedTodayTrend() { return resolvedTodayTrend; }
    public void setResolvedTodayTrend(int resolvedTodayTrend) { this.resolvedTodayTrend = resolvedTodayTrend; }
    public int getPendingFaqsTrend() { return pendingFaqsTrend; }
    public void setPendingFaqsTrend(int pendingFaqsTrend) { this.pendingFaqsTrend = pendingFaqsTrend; }
}
