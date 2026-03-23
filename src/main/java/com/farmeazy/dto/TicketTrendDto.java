package com.farmeazy.dto;

// Removed Lombok. Manual getters, setters, and constructors below.

public class TicketTrendDto {
    private String date;
    private long count;
    private long open;
    private long pending;
    private long inProgress;
    private long resolved;
    private long closed;
    private long cancelled;

    public TicketTrendDto() {}

    public TicketTrendDto(String date, long count) {
        this.date = date;
        this.count = count;
    }

    public TicketTrendDto(String date, long open, long pending, long inProgress, long resolved, long closed, long cancelled) {
        this.date = date;
        this.open = open;
        this.pending = pending;
        this.inProgress = inProgress;
        this.resolved = resolved;
        this.closed = closed;
        this.cancelled = cancelled;
        this.count = open + pending + inProgress + resolved + closed + cancelled;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    public long getOpen() { return open; }
    public void setOpen(long open) { this.open = open; }

    public long getPending() { return pending; }
    public void setPending(long pending) { this.pending = pending; }

    public long getInProgress() { return inProgress; }
    public void setInProgress(long inProgress) { this.inProgress = inProgress; }

    public long getResolved() { return resolved; }
    public void setResolved(long resolved) { this.resolved = resolved; }

    public long getClosed() { return closed; }
    public void setClosed(long closed) { this.closed = closed; }

    public long getCancelled() { return cancelled; }
    public void setCancelled(long cancelled) { this.cancelled = cancelled; }
}
