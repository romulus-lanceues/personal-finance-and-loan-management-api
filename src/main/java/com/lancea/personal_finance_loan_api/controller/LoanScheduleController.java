package com.lancea.personal_finance_loan_api.controller;

import com.lancea.personal_finance_loan_api.dto.response.LoanScheduleResponse;
import com.lancea.personal_finance_loan_api.service.LoanScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans/{loanId}/schedule")
@RequiredArgsConstructor
public class LoanScheduleController {

    private final LoanScheduleService loanScheduleService;

    @GetMapping
    public ResponseEntity<List<LoanScheduleResponse>> getLoanSchedules (@PathVariable UUID loanId,
                                                                        @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(loanScheduleService.getLoanSchedules(loanId, jwt));
    }

    @GetMapping("/{paymentNumber}")
    public ResponseEntity<LoanScheduleResponse> getSpecificLoanSchedule(@PathVariable UUID loanId,
                                                                        @PathVariable int paymentNumber,
                                                                        @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(loanScheduleService
                .getSpecificLoanSchedule(loanId, paymentNumber, jwt));
    }
}
