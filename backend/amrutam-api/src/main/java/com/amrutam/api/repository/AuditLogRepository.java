package com.amrutam.api.repository;

import com.amrutam.api.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * JPA repository for {@link AuditLog} (api owns this table; workers append
 * via {@code JdbcTemplate} per the shared-JPA rule in
 * {@code docs/ARCHITECTURE.md}).
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
