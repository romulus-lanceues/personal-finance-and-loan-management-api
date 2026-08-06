package com.lancea.personal_finance_loan_api.controller;

import com.lancea.personal_finance_loan_api.dto.request.LoanRequest;
import com.lancea.personal_finance_loan_api.dto.response.LoanComparisonResponse;
import com.lancea.personal_finance_loan_api.dto.response.LoanResponse;
import com.lancea.personal_finance_loan_api.dto.response.PagedLoanResponse;
import com.lancea.personal_finance_loan_api.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    public ResponseEntity<LoanResponse> createLoan(@RequestBody LoanRequest loanRequest,
                                                   @AuthenticationPrincipal Jwt jwt){

        LoanResponse response = loanService.createLoan(loanRequest, jwt);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.loanId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<PagedLoanResponse> getUserLoans(@PageableDefault(page = 0, size = 10, sort = "disbursedAt") Pageable pageable,
                                                      @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(loanService.getUserLoans(pageable,jwt));
    }

    @GetMapping("/{loanId}")
    ResponseEntity<LoanResponse> getLoanById(@PathVariable UUID loanId,
                                             @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(loanService.getLoanById(loanId, jwt));
    }

    @GetMapping("/compare")
    ResponseEntity<LoanComparisonResponse> compareLoans(@RequestParam UUID loanAId,
                                                        @RequestParam UUID loanBId,
                                                        @AuthenticationPrincipal Jwt jwt){

        return ResponseEntity.ok(loanService.compareLoan(loanAId, loanBId, jwt));
    }



}
