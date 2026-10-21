package com.amrutam.api.entity;

/**
 * States of an {@link AsyncJob}.
 */
public enum JobStatus {
    /** Waiting to be picked up. */ PENDING,
    /** Currently leased to a worker. */ IN_PROGRESS,
    /** Completed successfully. */ DONE,
    /** Failed but retryable. */ FAILED,
    /** Exceeded max attempts – dead-lettered. */ DEAD
}
