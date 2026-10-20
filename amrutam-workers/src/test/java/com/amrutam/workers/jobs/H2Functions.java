package com.amrutam.workers.jobs;

import java.sql.Timestamp;

/**
 * Test-only H2 function aliases. H2 (even in PostgreSQL mode) does not
 * provide a {@code now()} function by default; the production
 * {@code JdbcTemplateJobStore} SQL uses the PostgreSQL {@code now()}
 * idiom, so this class supplies a static method H2 can alias to keep the
 * production SQL portable across both databases in the test suite.
 */
public final class H2Functions {

    private H2Functions() {
    }

    /**
     * @return the current timestamp (epoch-millis based), matching the
     *         semantics of PostgreSQL {@code now()} for test purposes
     */
    public static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }
}
