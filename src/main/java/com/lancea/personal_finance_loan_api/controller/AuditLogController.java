package com.lancea.personal_finance_loan_api.controller;

import com.lancea.personal_finance_loan_api.dto.response.AuditLogResponse;
import com.lancea.personal_finance_loan_api.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> getUserAudit(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(auditLogService.getUserAudit(jwt));
    }

    @GetMapping("/entity/{entityId}")
    public ResponseEntity<List<AuditLogResponse>> getAuditsByEntityId(@PathVariable UUID entityId){
        return ResponseEntity.ok(auditLogService.getAuditsByEntityId(entityId));
    }
}
