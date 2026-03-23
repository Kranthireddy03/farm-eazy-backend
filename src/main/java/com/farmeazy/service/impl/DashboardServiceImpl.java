package com.farmeazy.service.impl;

import com.farmeazy.dto.DashboardStatsDto;
import com.farmeazy.dto.TicketDto;
import com.farmeazy.dto.FaqDto;
import com.farmeazy.dto.TicketTrendDto;
import com.farmeazy.entity.Ticket;
import com.farmeazy.entity.FAQQuestion;
import com.farmeazy.repository.TicketRepository;
import com.farmeazy.repository.FAQQuestionRepository;
import com.farmeazy.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private FAQQuestionRepository faqRepository;

    private OffsetDateTime getStartDateOffset(String filter) {
        switch (filter == null ? "" : filter.toLowerCase()) {
            case "week":
                return OffsetDateTime.now().minusDays(7);
            case "month":
                return OffsetDateTime.now().minusDays(30);
            default:
                return OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
    }

    private java.time.LocalDateTime getStartDateLocal(String filter) {
        switch (filter == null ? "" : filter.toLowerCase()) {
            case "week":
                return java.time.LocalDateTime.now().minusDays(7);
            case "month":
                return java.time.LocalDateTime.now().minusDays(30);
            default:
                return java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
    }

    private String normalizeSource(String source) {
        if (source == null) return "all";
        source = source.trim().toLowerCase();
        if (!source.equals("all") && !source.equals("public") && !source.equals("login")) {
            return "all";
        }
        return source;
    }

    private OffsetDateTime getCurrentPeriodStart(String filter) {
        OffsetDateTime now = OffsetDateTime.now();
        if (filter == null || filter.equalsIgnoreCase("today")) {
            return now.toLocalDate().atStartOfDay().atOffset(now.getOffset());
        }
        if (filter.equalsIgnoreCase("week")) {
            return now.minusDays(7);
        }
        if (filter.equalsIgnoreCase("month")) {
            return now.minusDays(30);
        }
        if (filter.equalsIgnoreCase("year")) {
            return now.minusDays(365);
        }
        // Default fallback to the last 30 days for unknown filter values
        return now.minusDays(30);
    }

    private OffsetDateTime getPreviousPeriodStart(String filter) {
        OffsetDateTime currentStart = getCurrentPeriodStart(filter);
        if (filter == null || filter.equalsIgnoreCase("today")) {
            return currentStart.minusDays(1);
        }
        if (filter.equalsIgnoreCase("week")) {
            return currentStart.minusDays(7);
        }
        if (filter.equalsIgnoreCase("month")) {
            return currentStart.minusDays(30);
        }
        if (filter.equalsIgnoreCase("year")) {
            return currentStart.minusDays(365);
        }
        return currentStart.minusDays(30);
    }

    private int calculatePercentageChange(long previous, long current) {
        if (previous == 0) {
            return current == 0 ? 0 : 100;
        }
        double change = ((double) (current - previous) / previous) * 100;
        return (int) Math.round(change);
    }

    private Specification<Ticket> ticketSourceSpecification(String source) {
        if ("public".equals(source)) {
            return (root, query, cb) -> cb.or(
                    cb.isNull(root.get("createdBy")),
                    cb.equal(root.get("createdBy"), 0L)
            );
        }
        if ("login".equals(source)) {
            return (root, query, cb) -> cb.and(
                    cb.isNotNull(root.get("createdBy")),
                    cb.notEqual(root.get("createdBy"), 0L)
            );
        }
        return null;
    }

    private String normalizeStatus(String status) {
        if (status == null) return "all";
        status = status.trim().toLowerCase();
        switch (status) {
            case "all":
            case "open":
            case "pending":
            case "in_progress":
            case "resolved":
            case "closed":
            case "cancelled":
            case "archived":
                return status;
            default:
                return "all";
        }
    }

    private Specification<Ticket> ticketStatusSpecification(String status) {
        if ("open".equals(status)) {
            return (root, query, cb) -> cb.not(root.get("status").in(Arrays.asList("RESOLVED", "CLOSED", "CANCELLED", "ARCHIVED")));
        }
        if (!"all".equals(status)) {
            return (root, query, cb) -> cb.equal(cb.upper(root.get("status")), status.toUpperCase());
        }
        return null;
    }

    private OffsetDateTime getEarliestTicketCreatedAt(String source) {
        if ("public".equals(source)) {
            OffsetDateTime dt = ticketRepository.findEarliestPublicCreatedAt();
            return dt != null ? dt : OffsetDateTime.now();
        }
        if ("login".equals(source)) {
            OffsetDateTime dt = ticketRepository.findEarliestLoginCreatedAt();
            return dt != null ? dt : OffsetDateTime.now();
        }
        OffsetDateTime dt = ticketRepository.findEarliestCreatedAt();
        return dt != null ? dt : OffsetDateTime.now();
    }

    private Specification<FAQQuestion> faqSourceSpecification(String source) {
        if ("public".equals(source)) {
            return (root, query, cb) -> cb.or(
                    cb.isNull(root.get("userId")),
                    cb.equal(root.get("userId"), "")
            );
        }
        if ("login".equals(source)) {
            return (root, query, cb) -> cb.and(
                    cb.isNotNull(root.get("userId")),
                    cb.notEqual(root.get("userId"), "")
            );
        }
        return null;
    }

    @Override
    public DashboardStatsDto getStats(String filter, String source, String status) {
        String normalizedSource = normalizeSource(source);
        String normalizedStatus = normalizeStatus(status);
        OffsetDateTime now = OffsetDateTime.now();

        OffsetDateTime windowStart = "all".equalsIgnoreCase(filter) ? null : getCurrentPeriodStart(filter);
        OffsetDateTime prevWindowStart = null;
        OffsetDateTime prevWindowEnd = null;

        if (windowStart != null) {
            prevWindowEnd = windowStart;
            prevWindowStart = getPreviousPeriodStart(filter);
        }

        // make final copies for lambdas to avoid Java effective-final requirements
        final OffsetDateTime finalNow = now;
        final OffsetDateTime finalWindowStart = windowStart;

        final OffsetDateTime finalPrevWindowStart = prevWindowStart;
        final OffsetDateTime finalPrevWindowEnd = prevWindowEnd;

        Specification<Ticket> baseTicketSpec = Specification.where(null);
        if (!"all".equals(normalizedSource)) {
            baseTicketSpec = baseTicketSpec.and(ticketSourceSpecification(normalizedSource));
        }
        if (!"all".equals(normalizedStatus)) {
            baseTicketSpec = baseTicketSpec.and(ticketStatusSpecification(normalizedStatus));
        }

        // total in current window (or all time)
        Specification<Ticket> totalTicketSpec = baseTicketSpec;
        if (finalWindowStart != null) {
            totalTicketSpec = totalTicketSpec.and((root, query, cb) -> cb.between(root.get("createdAt"), finalWindowStart, finalNow));
        }
        long totalTickets = ticketRepository.count(totalTicketSpec);

        // pending tickets in current window (or all time)
        Specification<Ticket> pendingTicketSpec = totalTicketSpec.and((root, query, cb) -> cb.equal(cb.upper(root.get("status")), "PENDING"));
        long pendingTickets = ticketRepository.count(pendingTicketSpec);

        // resolved in current window (or all time)
        Specification<Ticket> resolvedTicketSpec = baseTicketSpec.and((root, query, cb) -> cb.equal(cb.upper(root.get("status")), "RESOLVED"));
        if (finalWindowStart != null) {
            resolvedTicketSpec = resolvedTicketSpec.and((root, query, cb) -> cb.between(root.get("updatedAt"), finalWindowStart, finalNow));
        }
        long resolvedToday = ticketRepository.count(resolvedTicketSpec);

        Specification<FAQQuestion> faqSpec = Specification.where((root, query, cb) -> cb.isNull(root.get("answer")));
        if (!"all".equals(normalizedSource)) {
            faqSpec = faqSpec.and(faqSourceSpecification(normalizedSource));
        }

        Specification<FAQQuestion> pendingFaqSpec = faqSpec;
        if (finalWindowStart != null) {
            pendingFaqSpec = pendingFaqSpec.and((root, query, cb) -> cb.between(root.get("submittedAt"), finalWindowStart, finalNow));
        }
        long pendingFaqs = faqRepository.count(pendingFaqSpec);

        // Trend calc using previous same interval
        long prevTotal = 0;
        long prevPending = 0;
        long prevResolved = 0;
        long prevFaqs = 0;

        if (finalPrevWindowStart != null && finalPrevWindowEnd != null) {
            Specification<Ticket> prevTotalSpec = baseTicketSpec.and((root, query, cb) -> cb.between(root.get("createdAt"), finalPrevWindowStart, finalPrevWindowEnd));
            prevTotal = ticketRepository.count(prevTotalSpec);

            Specification<Ticket> prevPendingSpec = prevTotalSpec.and((root, query, cb) -> cb.equal(cb.upper(root.get("status")), "PENDING"));
            prevPending = ticketRepository.count(prevPendingSpec);

            Specification<Ticket> prevResolvedSpec = baseTicketSpec
                    .and((root, query, cb) -> cb.equal(cb.upper(root.get("status")), "RESOLVED"))
                    .and((root, query, cb) -> cb.between(root.get("updatedAt"), finalPrevWindowStart, finalPrevWindowEnd));
            prevResolved = ticketRepository.count(prevResolvedSpec);

            Specification<FAQQuestion> prevFaqSpec = faqSpec.and((root, query, cb) -> cb.between(root.get("submittedAt"), finalPrevWindowStart, finalPrevWindowEnd));
            prevFaqs = faqRepository.count(prevFaqSpec);
        }

        int totalTrend = calculatePercentageChange(prevTotal, totalTickets);
        int pendingTrend = calculatePercentageChange(prevPending, pendingTickets);
        int resolvedTrend = calculatePercentageChange(prevResolved, resolvedToday);
        int faqTrend = calculatePercentageChange(prevFaqs, pendingFaqs);

        return new DashboardStatsDto(
                (int) totalTickets,
                (int) pendingTickets,
                (int) resolvedToday,
                (int) pendingFaqs,
                totalTrend,
                pendingTrend,
                resolvedTrend,
                faqTrend
        );
    }

    @Override
    public List<TicketDto> getRecentTickets(String filter, String source, String status) {
        String normalizedSource = normalizeSource(source);
        String normalizedStatus = normalizeStatus(status);

        Specification<Ticket> spec = Specification.where(null);

        if (!"all".equals(normalizedSource)) {
            spec = spec.and(ticketSourceSpecification(normalizedSource));
        }
        if (!"all".equals(normalizedStatus)) {
            spec = spec.and(ticketStatusSpecification(normalizedStatus));
        }

        if (filter != null && !filter.equalsIgnoreCase("all")) {
            OffsetDateTime start = getStartDateOffset(filter);
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), start));
        }

        List<Ticket> tickets = ticketRepository.findAll(spec, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();

        List<TicketDto> result = new ArrayList<>();
        for (Ticket t : tickets) {
            String sourceValue = (t.getCreatedBy() != null) ? "login" : "public";
            String priorityValue = t.getPriority() != null ? t.getPriority().toUpperCase() : "LOW";
            TicketDto dto = new TicketDto(t.getId(), t.getTitle(), t.getStatus(), t.getCreatedAt(), sourceValue, priorityValue);
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<FaqDto> getRecentFaqs(String filter, String source, String status) {
        String normalizedSource = normalizeSource(source);
        String normalizedStatus = (status == null) ? "all" : status.trim().toLowerCase();
        Specification<FAQQuestion> spec = Specification.where((root, query, cb) -> cb.isNull(root.get("answer")));

        // only unanswered for dashboard, but if status is 'answered' allow answered as well (if requested)
        if ("answered".equals(normalizedStatus)) {
            spec = Specification.where((root, query, cb) -> cb.isNotNull(root.get("answer")));
        } else if ("all".equals(normalizedStatus)) {
            spec = Specification.where(null);
        }

        if (!"all".equals(normalizedSource)) {
            spec = spec.and(faqSourceSpecification(normalizedSource));
        }

        if (filter != null && !filter.equalsIgnoreCase("all")) {
            OffsetDateTime start = getStartDateOffset(filter);
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("submittedAt"), start));
        }

        List<FAQQuestion> faqs = faqRepository.findAll(spec, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"))).getContent();

        List<FaqDto> result = new ArrayList<>();
        for (FAQQuestion f : faqs) {
            FaqDto dto = new FaqDto(f.getId(), f.getQuestion(), f.getAnswer());
            dto.setSource(f.getSource());
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<TicketTrendDto> getTicketsTrend(String filter, String source, String status) {
        String normalizedSource = normalizeSource(source);
        String normalizedStatus = normalizeStatus(status);
        List<TicketTrendDto> result = new ArrayList<>();

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime nowStartOfDay = now.toLocalDate().atStartOfDay().atOffset(now.getOffset());

        Specification<Ticket> baseSpec = Specification.where(null);
        if (!"all".equals(normalizedSource)) {
            baseSpec = baseSpec.and(ticketSourceSpecification(normalizedSource));
        }
        final Specification<Ticket> finalBaseSpec = baseSpec;

        // Use earliest ticket as start for 'all' and scroll history support
        OffsetDateTime earliest = getEarliestTicketCreatedAt(normalizedSource);
        if (earliest == null) {
            earliest = nowStartOfDay;
        }

        // helper to count by status in range with source filtering
        final var statusList = Arrays.asList("OPEN", "PENDING", "IN_PROGRESS", "RESOLVED", "CLOSED", "CANCELLED");

        java.util.function.BiFunction<String, java.time.OffsetDateTime[], Long> statusCount = (statusFilter, range) -> {
            Specification<Ticket> statusSpec = Specification.where((root, query, cb) -> cb.equal(cb.upper(root.get("status")), statusFilter));
            if (finalBaseSpec != null) {
                statusSpec = finalBaseSpec.and(statusSpec);
            }
            if (range != null && range.length == 2 && range[0] != null && range[1] != null) {
                statusSpec = statusSpec.and((root, query, cb) -> cb.between(root.get("createdAt"), range[0], range[1]));
            }
            return ticketRepository.count(statusSpec);
        };

        switch (filter == null ? "all" : filter.toLowerCase()) {
            case "today": {
                OffsetDateTime rangeStart = earliest.isBefore(nowStartOfDay.minusDays(6)) ? nowStartOfDay.minusDays(6) : earliest;
                int hours = (int) java.time.Duration.between(rangeStart, now).toHours();
                for (int i = hours; i >= 0; i--) {
                    OffsetDateTime start = now.minusHours(i);
                    OffsetDateTime end = start.plusHours(1);
                    long open = statusCount.apply("OPEN", new OffsetDateTime[]{start, end});
                    long pending = statusCount.apply("PENDING", new OffsetDateTime[]{start, end});
                    long inProgress = statusCount.apply("IN_PROGRESS", new OffsetDateTime[]{start, end});
                    long resolved = statusCount.apply("RESOLVED", new OffsetDateTime[]{start, end});
                    long closed = statusCount.apply("CLOSED", new OffsetDateTime[]{start, end});
                    long cancelled = statusCount.apply("CANCELLED", new OffsetDateTime[]{start, end});
                    result.add(new TicketTrendDto(start.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")), open, pending, inProgress, resolved, closed, cancelled));
                }
                break;
            }
            case "week": {
                LocalDate existingWeekStart = nowStartOfDay.toLocalDate().with(java.time.DayOfWeek.MONDAY);
                LocalDate fullStartWeek = earliest.toLocalDate().with(java.time.DayOfWeek.MONDAY);
                long weekCount = java.time.temporal.ChronoUnit.WEEKS.between(fullStartWeek, existingWeekStart) + 1;
                for (long i = weekCount - 1; i >= Math.max(0, weekCount - 12); i--) {
                    LocalDate weekStart = fullStartWeek.plusWeeks(i);
                    LocalDate weekEnd = weekStart.plusWeeks(1);
                    OffsetDateTime start = weekStart.atStartOfDay().atOffset(now.getOffset());
                    OffsetDateTime end = weekEnd.atStartOfDay().atOffset(now.getOffset());
                    long open = statusCount.apply("OPEN", new OffsetDateTime[]{start, end});
                    long pending = statusCount.apply("PENDING", new OffsetDateTime[]{start, end});
                    long inProgress = statusCount.apply("IN_PROGRESS", new OffsetDateTime[]{start, end});
                    long resolved = statusCount.apply("RESOLVED", new OffsetDateTime[]{start, end});
                    long closed = statusCount.apply("CLOSED", new OffsetDateTime[]{start, end});
                    long cancelled = statusCount.apply("CANCELLED", new OffsetDateTime[]{start, end});
                    result.add(new TicketTrendDto("Wk " + weekStart.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR), open, pending, inProgress, resolved, closed, cancelled));
                }
                break;
            }
            case "month": {
                java.time.YearMonth currentMonth = java.time.YearMonth.from(now);
                java.time.YearMonth minMonth = java.time.YearMonth.from(earliest);
                long monthCount = java.time.temporal.ChronoUnit.MONTHS.between(minMonth, currentMonth) + 1;
                for (long i = monthCount - 1; i >= Math.max(0, monthCount - 12); i--) {
                    java.time.YearMonth month = minMonth.plusMonths(i);
                    OffsetDateTime start = month.atDay(1).atStartOfDay().atOffset(now.getOffset());
                    OffsetDateTime end = month.plusMonths(1).atDay(1).atStartOfDay().atOffset(now.getOffset());
                    long open = statusCount.apply("OPEN", new OffsetDateTime[]{start, end});
                    long pending = statusCount.apply("PENDING", new OffsetDateTime[]{start, end});
                    long inProgress = statusCount.apply("IN_PROGRESS", new OffsetDateTime[]{start, end});
                    long resolved = statusCount.apply("RESOLVED", new OffsetDateTime[]{start, end});
                    long closed = statusCount.apply("CLOSED", new OffsetDateTime[]{start, end});
                    long cancelled = statusCount.apply("CANCELLED", new OffsetDateTime[]{start, end});
                    result.add(new TicketTrendDto(month.toString(), open, pending, inProgress, resolved, closed, cancelled));
                }
                break;
            }
            case "year": {
                int currentYear = now.getYear();
                int startYear = earliest.getYear();
                for (int yr = currentYear; yr >= Math.max(startYear, currentYear - 9); yr--) {
                    OffsetDateTime start = OffsetDateTime.of(java.time.LocalDate.of(yr, 1, 1), java.time.LocalTime.MIN, now.getOffset());
                    OffsetDateTime end = OffsetDateTime.of(java.time.LocalDate.of(yr + 1, 1, 1), java.time.LocalTime.MIN, now.getOffset());
                    long open = statusCount.apply("OPEN", new OffsetDateTime[]{start, end});
                    long pending = statusCount.apply("PENDING", new OffsetDateTime[]{start, end});
                    long inProgress = statusCount.apply("IN_PROGRESS", new OffsetDateTime[]{start, end});
                    long resolved = statusCount.apply("RESOLVED", new OffsetDateTime[]{start, end});
                    long closed = statusCount.apply("CLOSED", new OffsetDateTime[]{start, end});
                    long cancelled = statusCount.apply("CANCELLED", new OffsetDateTime[]{start, end});
                    result.add(new TicketTrendDto(String.valueOf(yr), open, pending, inProgress, resolved, closed, cancelled));
                }
                break;
            }
            default: {
                java.time.YearMonth startMonth = java.time.YearMonth.from(earliest);
                java.time.YearMonth nowMonth = java.time.YearMonth.from(now);
                long monthCount = java.time.temporal.ChronoUnit.MONTHS.between(startMonth, nowMonth) + 1;
                long maxMonths = Math.max(24, monthCount);
                long displayMonths = Math.min(maxMonths, monthCount);
                for (long i = monthCount - displayMonths; i < monthCount; i++) {
                    java.time.YearMonth month = startMonth.plusMonths(i);
                    OffsetDateTime start = month.atDay(1).atStartOfDay().atOffset(now.getOffset());
                    OffsetDateTime end = month.plusMonths(1).atDay(1).atStartOfDay().atOffset(now.getOffset());
                    long open = statusCount.apply("OPEN", new OffsetDateTime[]{start, end});
                    long pending = statusCount.apply("PENDING", new OffsetDateTime[]{start, end});
                    long inProgress = statusCount.apply("IN_PROGRESS", new OffsetDateTime[]{start, end});
                    long resolved = statusCount.apply("RESOLVED", new OffsetDateTime[]{start, end});
                    long closed = statusCount.apply("CLOSED", new OffsetDateTime[]{start, end});
                    long cancelled = statusCount.apply("CANCELLED", new OffsetDateTime[]{start, end});
                    result.add(new TicketTrendDto(month.toString(), open, pending, inProgress, resolved, closed, cancelled));
                }
                break;
            }
        }

        return result;
    }
}

