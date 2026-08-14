package com.lancea.personal_finance_loan_api.repository;

import com.lancea.personal_finance_loan_api.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByUserIdOrderByPerformedAtDesc(UUID userId);

    List<AuditLog> findByEntityIdOrderByPerformedAt(UUID entityId);
}
