package com.lancea.personal_finance_loan_api.dto.response;

import com.lancea.personal_finance_loan_api.entity.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        Long id,
        String action,
        String entityType,
        UUID entityId,
        String details,
        Instant performedAt
) {

    public static AuditLogResponse of(AuditLog auditLog){
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getDetails(),
                auditLog.getPerformedAt()
        );
    }
}
