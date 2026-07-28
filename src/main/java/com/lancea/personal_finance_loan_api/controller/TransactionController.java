package com.lancea.personal_finance_loan_api.controller;

import com.lancea.personal_finance_loan_api.dto.request.DepositRequest;
import com.lancea.personal_finance_loan_api.dto.response.TransactionResponse;
import com.lancea.personal_finance_loan_api.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest depositRequest,
                                                                @AuthenticationPrincipal Jwt jwt){

        return ResponseEntity.ok(transactionService.deposit(depositRequest, jwt));
    }

}
