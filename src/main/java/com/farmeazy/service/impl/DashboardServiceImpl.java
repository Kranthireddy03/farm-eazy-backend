package com.farmeazy.service.impl;

import com.farmeazy.dto.DashboardStatsDto;
import com.farmeazy.dto.FaqDto;
import com.farmeazy.dto.TicketDto;
import com.farmeazy.dto.TicketTrendDto;
import com.farmeazy.entity.FAQQuestion;
import com.farmeazy.entity.SupportTicket;
import com.farmeazy.repository.FAQQuestionRepository;
import com.farmeazy.repository.SupportTicketRepository;
import com.farmeazy.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private SupportTicketRepository supportTicketRepository;

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

    private LocalDateTime getStartDateLocal(String filter) {
        switch (filter == null ? "" : filter.toLowerCase()) {
            case "week":
                return LocalDateTime.now().minusDays(7);
            case "month":
                return LocalDateTime.now().minusDays(30);
            default:
                return LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
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

    private LocalDateTime getCurrentPeriodStartLocal(String filter) {
        LocalDateTime now = LocalDateTime.now();
        if (filter == null || filter.equalsIgnoreCase("today")) {
            return now.toLocalDate().atStartOfDay();
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

    private LocalDateTime getPreviousPeriodStartLocal(String filter) {
        LocalDateTime currentStart = getCurrentPeriodStartLocal(filter);
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

    private Specification<SupportTicket> ticketSourceSpecification(String source) {
        if ("public".equals(source)) {
            return (root, query, cb) -> cb.or(
                    cb.equal(cb.lower(root.get("source")), "public"),
                    cb.and(cb.isNull(root.get("source")), cb.isNull(root.get("user")))
            );
        }
        if ("login".equals(source)) {
            return (root, query, cb) -> cb.or(
                    cb.equal(cb.lower(root.get("source")), "login"),
                    cb.isNotNull(root.get("user"))
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
            case "pending_user":
            case "in_progress":
            case "resolved":
            case "closed":
            case "cancelled":
                return status;
            default:
                return "all";
        }
    }

    private Specification<SupportTicket> ticketStatusSpecification(String status) {
        if ("open".equals(status)) {
            return (root, query, cb) -> cb.not(root.get("status").in(Arrays.asList(
                    SupportTicket.TicketStatus.RESOLVED,
                    SupportTicket.TicketStatus.CLOSED,
                    SupportTicket.TicketStatus.CANCELLED
            )));
        }
        if ("pending".equals(status)) {
            return (root, query, cb) -> cb.equal(root.get("status"), SupportTicket.TicketStatus.PENDING_USER);
        }
        if (!"all".equals(status)) {
            return (root, query, cb) -> cb.equal(cb.upper(root.get("status").as(String.class)), status.toUpperCase());
        }
        return null;
    }

    private LocalDateTime getEarliestTicketCreatedAt(String source) {
        if ("public".equals(source)) {
            LocalDateTime dt = supportTicketRepository.findEarliestPublicCreatedAt();
            return dt != null ? dt : LocalDateTime.now();
        }
        if ("login".equals(source)) {
            LocalDateTime dt = supportTicketRepository.findEarliestLoginCreatedAt();
            return dt != null ? dt : LocalDateTime.now();
        }
        LocalDateTime dt = supportTicketRepository.findEarliestCreatedAt();
        return dt != null ? dt : LocalDateTime.now();
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        return localDateTime.atOffset(ZoneOffset.UTC);
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
        LocalDateTime now = LocalDateTime.now();
        OffsetDateTime nowOffset = OffsetDateTime.now();

        LocalDateTime windowStart = "all".equalsIgnoreCase(filter) ? null : getCurrentPeriodStartLocal(filter);
        LocalDateTime prevWindowStart = null;
        LocalDateTime prevWindowEnd = null;

        if (windowStart != null) {
            prevWindowEnd = windowStart;
            prevWindowStart = getPreviousPeriodStartLocal(filter);
        }

        // make final copies for lambdas to avoid Java effective-final requirements
        final LocalDateTime finalNow = now;
        final LocalDateTime finalWindowStart = windowStart;

        final LocalDateTime finalPrevWindowStart = prevWindowStart;
        final LocalDateTime finalPrevWindowEnd = prevWindowEnd;

        Specification<SupportTicket> baseTicketSpec = Specification.where(null);
        if (!"all".equals(normalizedSource)) {
            baseTicketSpec = baseTicketSpec.and(ticketSourceSpecification(normalizedSource));
        }
        if (!"all".equals(normalizedStatus)) {
            baseTicketSpec = baseTicketSpec.and(ticketStatusSpecification(normalizedStatus));
        }

        // total in current window (or all time)
        Specification<SupportTicket> totalTicketSpec = baseTicketSpec;
        if (finalWindowStart != null) {
            totalTicketSpec = totalTicketSpec.and((root, query, cb) -> cb.between(root.get("createdAt"), finalWindowStart, finalNow));
        }
        long totalTickets = supportTicketRepository.count(totalTicketSpec);

        // pending tickets in current window (or all time)
        Specification<SupportTicket> pendingTicketSpec = totalTicketSpec.and((root, query, cb) -> cb.equal(root.get("status"), SupportTicket.TicketStatus.PENDING_USER));
        long pendingTickets = supportTicketRepository.count(pendingTicketSpec);

        // resolved in current window (or all time)
        Specification<SupportTicket> resolvedTicketSpec = baseTicketSpec.and((root, query, cb) -> cb.equal(root.get("status"), SupportTicket.TicketStatus.RESOLVED));
        if (finalWindowStart != null) {
            resolvedTicketSpec = resolvedTicketSpec.and((root, query, cb) -> cb.between(root.get("updatedAt"), finalWindowStart, finalNow));
        }
        long resolvedToday = supportTicketRepository.count(resolvedTicketSpec);

        Specification<FAQQuestion> faqSpec = Specification.where((root, query, cb) -> cb.isNull(root.get("answer")));
        if (!"all".equals(normalizedSource)) {
            faqSpec = faqSpec.and(faqSourceSpecification(normalizedSource));
        }

        Specification<FAQQuestion> pendingFaqSpec = faqSpec;
        if (finalWindowStart != null) {
            pendingFaqSpec = pendingFaqSpec.and((root, query, cb) -> cb.between(root.get("submittedAt"), finalWindowStart.atOffset(ZoneOffset.UTC), nowOffset));
        }
        long pendingFaqs = faqRepository.count(pendingFaqSpec);

        // Trend calc using previous same interval
        long prevTotal = 0;
        long prevPending = 0;
        long prevResolved = 0;
        long prevFaqs = 0;

        if (finalPrevWindowStart != null && finalPrevWindowEnd != null) {
            Specification<SupportTicket> prevTotalSpec = baseTicketSpec.and((root, query, cb) -> cb.between(root.get("createdAt"), finalPrevWindowStart, finalPrevWindowEnd));
            prevTotal = supportTicketRepository.count(prevTotalSpec);

            Specification<SupportTicket> prevPendingSpec = prevTotalSpec.and((root, query, cb) -> cb.equal(root.get("status"), SupportTicket.TicketStatus.PENDING_USER));
            prevPending = supportTicketRepository.count(prevPendingSpec);

            Specification<SupportTicket> prevResolvedSpec = baseTicketSpec
                .and((root, query, cb) -> cb.equal(root.get("status"), SupportTicket.TicketStatus.RESOLVED))
                    .and((root, query, cb) -> cb.between(root.get("updatedAt"), finalPrevWindowStart, finalPrevWindowEnd));
            prevResolved = supportTicketRepository.count(prevResolvedSpec);

            Specification<FAQQuestion> prevFaqSpec = faqSpec.and((root, query, cb) -> cb.between(root.get("submittedAt"), finalPrevWindowStart.atOffset(ZoneOffset.UTC), finalPrevWindowEnd.atOffset(ZoneOffset.UTC)));
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

        Specification<SupportTicket> spec = Specification.where(null);

        if (!"all".equals(normalizedSource)) {
            spec = spec.and(ticketSourceSpecification(normalizedSource));
        }
        if (!"all".equals(normalizedStatus)) {
            spec = spec.and(ticketStatusSpecification(normalizedStatus));
        }

        if (filter != null && !filter.equalsIgnoreCase("all")) {
            LocalDateTime start = getStartDateLocal(filter);
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), start));
        }

        List<SupportTicket> tickets = supportTicketRepository.findAll(spec, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();

        List<TicketDto> result = new ArrayList<>();
        for (SupportTicket t : tickets) {
            String sourceValue = (t.getSource() != null && !t.getSource().isBlank())
                    ? t.getSource().toLowerCase()
                    : (t.getUser() != null ? "login" : "public");
            String priorityValue = t.getPriority() != null ? t.getPriority().name() : "LOW";
            TicketDto dto = new TicketDto(
                    t.getId(),
                    t.getSubject(),
                    t.getStatus() != null ? t.getStatus().name() : "OPEN",
                    toOffsetDateTime(t.getCreatedAt()),
                    sourceValue,
                    priorityValue
            );
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

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nowStartOfDay = now.toLocalDate().atStartOfDay();

        Specification<SupportTicket> baseSpec = Specification.where(null);
        if (!"all".equals(normalizedSource)) {
            baseSpec = baseSpec.and(ticketSourceSpecification(normalizedSource));
        }
        if (!"all".equals(normalizedStatus)) {
            baseSpec = baseSpec.and(ticketStatusSpecification(normalizedStatus));
        }
        final Specification<SupportTicket> finalBaseSpec = baseSpec;

        // Use earliest ticket as start for 'all' and scroll history support
        LocalDateTime earliest = getEarliestTicketCreatedAt(normalizedSource);
        if (earliest == null) {
            earliest = nowStartOfDay;
        }

        java.util.function.BiFunction<String, LocalDateTime[], Long> statusCount = (statusFilter, range) -> {
            String effectiveStatus = "PENDING".equals(statusFilter) ? "PENDING_USER" : statusFilter;
            Specification<SupportTicket> statusSpec = Specification.where((root, query, cb) -> cb.equal(cb.upper(root.get("status").as(String.class)), effectiveStatus));
            if (finalBaseSpec != null) {
                statusSpec = finalBaseSpec.and(statusSpec);
            }
            if (range != null && range.length == 2 && range[0] != null && range[1] != null) {
                statusSpec = statusSpec.and((root, query, cb) -> cb.between(root.get("createdAt"), range[0], range[1]));
            }
            return supportTicketRepository.count(statusSpec);
        };

        switch (filter == null ? "all" : filter.toLowerCase()) {
            case "today": {
                LocalDateTime rangeStart = earliest.isBefore(nowStartOfDay.minusDays(6)) ? nowStartOfDay.minusDays(6) : earliest;
                int hours = (int) java.time.Duration.between(rangeStart, now).toHours();
                for (int i = hours; i >= 0; i--) {
                    LocalDateTime start = now.minusHours(i);
                    LocalDateTime end = start.plusHours(1);
                    long open = statusCount.apply("OPEN", new LocalDateTime[]{start, end});
                    long pending = statusCount.apply("PENDING", new LocalDateTime[]{start, end});
                    long inProgress = statusCount.apply("IN_PROGRESS", new LocalDateTime[]{start, end});
                    long resolved = statusCount.apply("RESOLVED", new LocalDateTime[]{start, end});
                    long closed = statusCount.apply("CLOSED", new LocalDateTime[]{start, end});
                    long cancelled = statusCount.apply("CANCELLED", new LocalDateTime[]{start, end});
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
                    LocalDateTime start = weekStart.atStartOfDay();
                    LocalDateTime end = weekEnd.atStartOfDay();
                    long open = statusCount.apply("OPEN", new LocalDateTime[]{start, end});
                    long pending = statusCount.apply("PENDING", new LocalDateTime[]{start, end});
                    long inProgress = statusCount.apply("IN_PROGRESS", new LocalDateTime[]{start, end});
                    long resolved = statusCount.apply("RESOLVED", new LocalDateTime[]{start, end});
                    long closed = statusCount.apply("CLOSED", new LocalDateTime[]{start, end});
                    long cancelled = statusCount.apply("CANCELLED", new LocalDateTime[]{start, end});
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
                    LocalDateTime start = month.atDay(1).atStartOfDay();
                    LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
                    long open = statusCount.apply("OPEN", new LocalDateTime[]{start, end});
                    long pending = statusCount.apply("PENDING", new LocalDateTime[]{start, end});
                    long inProgress = statusCount.apply("IN_PROGRESS", new LocalDateTime[]{start, end});
                    long resolved = statusCount.apply("RESOLVED", new LocalDateTime[]{start, end});
                    long closed = statusCount.apply("CLOSED", new LocalDateTime[]{start, end});
                    long cancelled = statusCount.apply("CANCELLED", new LocalDateTime[]{start, end});
                    result.add(new TicketTrendDto(month.toString(), open, pending, inProgress, resolved, closed, cancelled));
                }
                break;
            }
            case "year": {
                int currentYear = now.getYear();
                int startYear = earliest.getYear();
                for (int yr = currentYear; yr >= Math.max(startYear, currentYear - 9); yr--) {
                    LocalDateTime start = java.time.LocalDate.of(yr, 1, 1).atStartOfDay();
                    LocalDateTime end = java.time.LocalDate.of(yr + 1, 1, 1).atStartOfDay();
                    long open = statusCount.apply("OPEN", new LocalDateTime[]{start, end});
                    long pending = statusCount.apply("PENDING", new LocalDateTime[]{start, end});
                    long inProgress = statusCount.apply("IN_PROGRESS", new LocalDateTime[]{start, end});
                    long resolved = statusCount.apply("RESOLVED", new LocalDateTime[]{start, end});
                    long closed = statusCount.apply("CLOSED", new LocalDateTime[]{start, end});
                    long cancelled = statusCount.apply("CANCELLED", new LocalDateTime[]{start, end});
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
                    LocalDateTime start = month.atDay(1).atStartOfDay();
                    LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
                    long open = statusCount.apply("OPEN", new LocalDateTime[]{start, end});
                    long pending = statusCount.apply("PENDING", new LocalDateTime[]{start, end});
                    long inProgress = statusCount.apply("IN_PROGRESS", new LocalDateTime[]{start, end});
                    long resolved = statusCount.apply("RESOLVED", new LocalDateTime[]{start, end});
                    long closed = statusCount.apply("CLOSED", new LocalDateTime[]{start, end});
                    long cancelled = statusCount.apply("CANCELLED", new LocalDateTime[]{start, end});
                    result.add(new TicketTrendDto(month.toString(), open, pending, inProgress, resolved, closed, cancelled));
                }
                break;
            }
        }

        return result;
    }
}

