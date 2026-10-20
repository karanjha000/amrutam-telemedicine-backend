package com.amrutam.api.service.impl;

import com.amrutam.api.dto.DailyConsultationsResponse;
import com.amrutam.api.dto.RevenueSummaryResponse;
import com.amrutam.api.dto.SystemHealthResponse;
import com.amrutam.api.entity.ConsultationStatus;
import com.amrutam.api.entity.JobStatus;
import com.amrutam.api.entity.PaymentStatus;
import com.amrutam.api.repository.AsyncJobRepository;
import com.amrutam.api.repository.ConsultationRepository;
import com.amrutam.api.repository.PaymentRepository;
import com.amrutam.api.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link AnalyticsService} implementation (admin-only, enforced at the
 * controller via {@code @PreAuthorize}).
 */
@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);

    private final ConsultationRepository consultations;
    private final PaymentRepository payments;
    private final AsyncJobRepository jobs;

    /**
     * @param consultations consultation repository
     * @param payments payment repository
     * @param jobs async job repository
     */
    public AnalyticsServiceImpl(ConsultationRepository consultations,
                                PaymentRepository payments,
                                AsyncJobRepository jobs) {
        this.consultations = consultations;
        this.payments = payments;
        this.jobs = jobs;
    }

    @Override
    @Transactional(readOnly = true)
    public DailyConsultationsResponse dailyConsultations(LocalDate date) {
        Instant from = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<Object[]> rows = consultations.countByDoctorScheduledAtBetween(from, to);
        Map<String, Long> perDoctor = new HashMap<>();
        long total = 0;
        for (Object[] row : rows) {
            Object doctorId = row[0];
            Number count = (Number) row[1];
            perDoctor.put(String.valueOf(doctorId), count.longValue());
            total += count.longValue();
        }
        log.debug("Daily consultations date={} total={} doctors={}", date, total, perDoctor.size());
        return new DailyConsultationsResponse(date.toString(), total, perDoctor);
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueSummaryResponse revenueSummary(LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        BigDecimal total = payments.sumCapturedBetween(start, end).orElse(BigDecimal.ZERO);
        long count = payments.countByStatusAndCreatedAtBetween(PaymentStatus.CAPTURED, start, end);
        return new RevenueSummaryResponse(total, count, "INR");
    }

    @Override
    @Transactional(readOnly = true)
    public SystemHealthResponse systemHealth() {
        Instant now = Instant.now();
        Instant dayAgo = now.minusSeconds(24L * 3600L);
        long activeSessions = consultations.countByStatusInAndScheduledAtBetween(
                List.of(ConsultationStatus.PAID, ConsultationStatus.COMPLETED), dayAgo, now);
        long pending = jobs.countByStatus(JobStatus.PENDING);
        long dead = jobs.countByStatus(JobStatus.DEAD);
        log.debug("System health active={} pendingJobs={} deadJobs={}", activeSessions, pending, dead);
        return new SystemHealthResponse(activeSessions, pending, dead);
    }
}
