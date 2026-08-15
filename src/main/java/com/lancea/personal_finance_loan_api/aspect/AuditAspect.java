package com.lancea.personal_finance_loan_api.aspect;

import com.lancea.personal_finance_loan_api.dto.response.AuditableInterface;
import com.lancea.personal_finance_loan_api.entity.Account;
import com.lancea.personal_finance_loan_api.entity.AuditLog;
import com.lancea.personal_finance_loan_api.entity.User;
import com.lancea.personal_finance_loan_api.repository.AccountRepository;
import com.lancea.personal_finance_loan_api.repository.AuditLogRepository;
import com.lancea.personal_finance_loan_api.repository.UserRepository;
import com.lancea.personal_finance_loan_api.utility.UserUtility;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Executable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AccountRepository accountRepository;

    @AfterReturning(value = "@annotation(auditableAnnotation)", returning = "result")
    public void createAudit(JoinPoint joinPoint, Auditable auditableAnnotation, Object result){

        try{
            User user = validateUser();
            String ipAddress = retrieveIp();
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
        catch (Exception e){
            log.warn("Exception encountered: ", e);
        }


    }

    private User validateUser(){
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UserUtility.getUserId(jwt);

        return userRepository.getReferenceById(userId);
    }

    private String retrieveIp(){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        return request.getRemoteAddr();
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
