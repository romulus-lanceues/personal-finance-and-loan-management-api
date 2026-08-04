package com.lancea.personal_finance_loan_api.controller;

import com.lancea.personal_finance_loan_api.dto.request.LoanRequest;
import com.lancea.personal_finance_loan_api.dto.response.LoanResponse;
import com.lancea.personal_finance_loan_api.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    public ResponseEntity<LoanResponse> createLoan(@RequestBody LoanRequest loanRequest,
                                                   @AuthenticationPrincipal Jwt jwt){

        return ResponseEntity.ok(loanService.createLoan(loanRequest, jwt));
    }

}
