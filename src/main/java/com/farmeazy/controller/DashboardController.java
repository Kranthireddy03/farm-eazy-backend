package com.farmeazy.controller;

import com.farmeazy.dto.DashboardStatsDto;
import com.farmeazy.dto.TicketDto;
import com.farmeazy.dto.FaqDto;
import com.farmeazy.dto.TicketTrendDto;
import com.farmeazy.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    // GET /admin/dashboard (legacy endpoint) or /admin/dashboard/stats
    @GetMapping({"", "/stats"})
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<DashboardStatsDto> getStats(
            @RequestParam(defaultValue = "today") String filter,
            @RequestParam(defaultValue = "all") String source,
            @RequestParam(defaultValue = "all") String status
    ) {
        return ResponseEntity.ok(dashboardService.getStats(filter, source, status));
    }

    // GET /admin/dashboard/recent-tickets?filter=week&source=public
    @GetMapping("/recent-tickets")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<TicketDto>> getRecentTickets(
            @RequestParam(defaultValue = "today") String filter,
            @RequestParam(defaultValue = "all") String source,
            @RequestParam(defaultValue = "all") String status
    ) {
        return ResponseEntity.ok(dashboardService.getRecentTickets(filter, source, status));
    }

    // GET /admin/dashboard/recent-faqs?filter=week&source=public
    @GetMapping("/recent-faqs")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<FaqDto>> getRecentFaqs(
            @RequestParam(defaultValue = "today") String filter,
            @RequestParam(defaultValue = "all") String source,
            @RequestParam(defaultValue = "all") String status
    ) {
        return ResponseEntity.ok(dashboardService.getRecentFaqs(filter, source, status));
    }

    // GET /admin/dashboard/tickets-trend?filter=week&source=public
    @GetMapping("/tickets-trend")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<TicketTrendDto>> getTicketsTrend(
            @RequestParam(defaultValue = "today") String filter,
            @RequestParam(defaultValue = "all") String source,
            @RequestParam(defaultValue = "all") String status
    ) {
        return ResponseEntity.ok(dashboardService.getTicketsTrend(filter, source, status));
    }
}
