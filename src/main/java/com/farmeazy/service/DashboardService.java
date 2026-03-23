package com.farmeazy.service;

import com.farmeazy.dto.DashboardStatsDto;
import com.farmeazy.dto.TicketDto;
import com.farmeazy.dto.FaqDto;
import com.farmeazy.dto.TicketTrendDto;
import java.util.List;

public interface DashboardService {
    DashboardStatsDto getStats(String filter, String source, String status);
    List<TicketDto> getRecentTickets(String filter, String source, String status);
    List<FaqDto> getRecentFaqs(String filter, String source, String status);
    List<TicketTrendDto> getTicketsTrend(String filter, String source, String status);
}
