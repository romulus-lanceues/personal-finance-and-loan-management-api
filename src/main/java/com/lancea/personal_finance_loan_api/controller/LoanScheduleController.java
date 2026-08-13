package com.lancea.personal_finance_loan_api.controller;

import com.lancea.personal_finance_loan_api.dto.request.LoanPaymentRequest;
import com.lancea.personal_finance_loan_api.dto.response.LoanPaymentResponse;
import com.lancea.personal_finance_loan_api.dto.response.LoanScheduleResponse;
import com.lancea.personal_finance_loan_api.service.LoanScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/{paymentNumber}/pay")
    public ResponseEntity<LoanPaymentResponse> payInstallment(@PathVariable UUID loanId,
                                                              @PathVariable int paymentNumber,
                                                              @Valid @RequestBody LoanPaymentRequest loanPaymentRequest,
                                                              @AuthenticationPrincipal Jwt jwt){

        return ResponseEntity.ok(loanScheduleService.payInstallment(loanId, paymentNumber, loanPaymentRequest, jwt));
    }

}
