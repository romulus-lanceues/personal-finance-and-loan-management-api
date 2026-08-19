package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.dto.response.AuditLogResponse;
import com.lancea.personal_finance_loan_api.entity.AuditLog;
import com.lancea.personal_finance_loan_api.repository.AuditLogRepository;
import com.lancea.personal_finance_loan_api.utility.UserUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public List<AuditLogResponse> getUserAudit(Jwt jwt){

        UUID userId = UserUtility.getUserId(jwt);

        List<AuditLog> auditLogs = auditLogRepository.findByUserIdOrderByPerformedAtDesc(userId);

        return auditLogs.stream().map(AuditLogResponse::of).toList();
    }

    public List<AuditLogResponse> getAuditsByEntityId(UUID entityId){

        List<AuditLog> auditLogs = auditLogRepository.findByEntityIdOrderByPerformedAt(entityId);

        return auditLogs.stream().map(AuditLogResponse::of).toList();
    }
}
