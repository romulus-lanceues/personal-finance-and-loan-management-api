package com.lancea.personal_finance_loan_api.aspect;

import com.lancea.personal_finance_loan_api.dto.response.AuditableInterface;
import com.lancea.personal_finance_loan_api.entity.User;
import com.lancea.personal_finance_loan_api.repository.UserRepository;
import com.lancea.personal_finance_loan_api.utility.UserUtility;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final UserRepository userRepository;
    private final AuditLogger auditLogger;


    @AfterReturning(value = "@annotation(auditableAnnotation)", returning = "result")
    public void auditAction(Auditable auditableAnnotation, Object result){

        try{
            User user = validateUser();
            String ipAddress = retrieveIp();

            if(result instanceof List<?> results){
                for(Object resultEntry : results){
                    if(resultEntry instanceof AuditableInterface){
                        auditLogger.generateAudit(auditableAnnotation, resultEntry, user, ipAddress);
                    }
                }
            }
            else{
                auditLogger.generateAudit(auditableAnnotation, result, user, ipAddress);
            }

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

}
