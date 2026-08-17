package com.lancea.personal_finance_loan_api.aspect;

import com.lancea.personal_finance_loan_api.dto.response.AuditableInterface;
import com.lancea.personal_finance_loan_api.entity.AuditLog;
import com.lancea.personal_finance_loan_api.entity.User;
import com.lancea.personal_finance_loan_api.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogger {

    private final ObjectMapper objectMapper;
    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateAudit (Auditable auditableAnnotation, Object result, User user, String ipAddress){

        String action = auditableAnnotation.action();
        String entityType = auditableAnnotation.entityType();
        AuditableInterface auditableResult = (AuditableInterface) result;

        UUID resultId =  auditableResult.getResultObjectId();
        Map<String, Object> details = auditableResult.generateAuditDetails();
        String serializedDetails = serializeDetails(details);

        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(resultId)
                .details(serializedDetails)
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(auditLog);
    }

    private String serializeDetails(Map<String, Object> details){
        try {
            return objectMapper.writeValueAsString(details);
        }
        catch (Exception e) {
            log.warn("Failed to serialize details: ", e);
            return "{}";
        }
    }

}
