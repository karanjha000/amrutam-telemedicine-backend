package com.amrutam.workers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the workers process, bound from the {@code amrutam.workers.*}
 * property namespace.
 *
 * <p>The relaxed binding also accepts the {@code amrutam.workers.poll-interval-ms}
 * and {@code amrutam.workers.saga-reconciliation-interval-ms} aliases used in
 * the {@code @Scheduled} fixed-delay placeholders (milliseconds, because
 * {@code fixedDelayString} takes a duration string). Defaults mirror the field
 * defaults below.</p>
 */
@ConfigurationProperties(prefix = "amrutam.workers")
public class WorkersProperties {

    /** Master switch: when {@code false} no worker component is active. */
    private boolean enabled = true;

    /** Polling interval (seconds) for the async_jobs table. */
    private int pollIntervalSeconds = 5;

    /** Maximum number of jobs claimed per poll cycle. */
    private int batchSize = 50;

    /** How long a claimed job stays IN_PROGRESS before another worker may steal it. */
    private long jobLeaseSeconds = 60;

    /** Interval (seconds) between booking-saga reconciliation sweeps. */
    private long sagaReconciliationIntervalSeconds = 30;

    /** A PENDING_PAYMENT consultation older than this is considered stuck. */
    private long sagaStuckAfterMinutes = 15;

    /** Base of the exponential retry backoff: nextRun = now + min(base * 2^attempt, max). */
    private long backoffBaseSeconds = 30;

    /** Cap for the exponential retry backoff. */
    private long backoffMaxSeconds = 900;

    /** Attempt ceiling before a job is dead-lettered. */
    private int maxRetries = 5;

    /** Default Redis channel for notification fan-out. */
    private String notificationChannel = "amrutam:notifications";

    /** Redis Stream that carries audit events into this process. */
    private String auditStream = "amrutam:audit-events";

    /**
     * @return master switch
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled the master switch
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return poll interval in seconds
     */
    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    /**
     * @param pollIntervalSeconds poll interval in seconds
     */
    public void setPollIntervalSeconds(int pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    /**
     * @return job batch size per poll
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * @param batchSize job batch size per poll
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * @return job lease length in seconds
     */
    public long getJobLeaseSeconds() {
        return jobLeaseSeconds;
    }

    /**
     * @param jobLeaseSeconds job lease length in seconds
     */
    public void setJobLeaseSeconds(long jobLeaseSeconds) {
        this.jobLeaseSeconds = jobLeaseSeconds;
    }

    /**
     * @return saga reconciliation interval in seconds
     */
    public long getSagaReconciliationIntervalSeconds() {
        return sagaReconciliationIntervalSeconds;
    }

    /**
     * @param sagaReconciliationIntervalSeconds saga reconciliation interval in seconds
     */
    public void setSagaReconciliationIntervalSeconds(long sagaReconciliationIntervalSeconds) {
        this.sagaReconciliationIntervalSeconds = sagaReconciliationIntervalSeconds;
    }

    /**
     * @return stuck-consultation age threshold in minutes
     */
    public long getSagaStuckAfterMinutes() {
        return sagaStuckAfterMinutes;
    }

    /**
     * @param sagaStuckAfterMinutes stuck-consultation age threshold in minutes
     */
    public void setSagaStuckAfterMinutes(long sagaStuckAfterMinutes) {
        this.sagaStuckAfterMinutes = sagaStuckAfterMinutes;
    }

    /**
     * @return exponential backoff base in seconds
     */
    public long getBackoffBaseSeconds() {
        return backoffBaseSeconds;
    }

    /**
     * @param backoffBaseSeconds exponential backoff base in seconds
     */
    public void setBackoffBaseSeconds(long backoffBaseSeconds) {
        this.backoffBaseSeconds = backoffBaseSeconds;
    }

    /**
     * @return exponential backoff cap in seconds
     */
    public long getBackoffMaxSeconds() {
        return backoffMaxSeconds;
    }

    /**
     * @param backoffMaxSeconds exponential backoff cap in seconds
     */
    public void setBackoffMaxSeconds(long backoffMaxSeconds) {
        this.backoffMaxSeconds = backoffMaxSeconds;
    }

    /**
     * @return maximum retry attempts before dead-lettering
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * @param maxRetries maximum retry attempts before dead-lettering
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * @return default notification channel
     */
    public String getNotificationChannel() {
        return notificationChannel;
    }

    /**
     * @param notificationChannel default notification channel
     */
    public void setNotificationChannel(String notificationChannel) {
        this.notificationChannel = notificationChannel;
    }

    /**
     * @return audit-events Redis Stream name
     */
    public String getAuditStream() {
        return auditStream;
    }

    /**
     * @param auditStream audit-events Redis Stream name
     */
    public void setAuditStream(String auditStream) {
        this.auditStream = auditStream;
    }
}
