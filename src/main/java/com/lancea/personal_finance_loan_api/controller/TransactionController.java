package com.lancea.personal_finance_loan_api.controller;

import com.lancea.personal_finance_loan_api.dto.request.DepositRequest;
import com.lancea.personal_finance_loan_api.dto.request.TransferRequest;
import com.lancea.personal_finance_loan_api.dto.request.WithdrawRequest;
import com.lancea.personal_finance_loan_api.dto.response.PagedTransactionResponse;
import com.lancea.personal_finance_loan_api.dto.response.TransactionResponse;
import com.lancea.personal_finance_loan_api.enums.FilterSearch;
import com.lancea.personal_finance_loan_api.enums.TransactionType;
import com.lancea.personal_finance_loan_api.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody WithdrawRequest withdrawRequest,
                                                        @AuthenticationPrincipal Jwt jwt){

        return ResponseEntity.ok(transactionService.withdraw(withdrawRequest, jwt));
    }

    @PostMapping("/transfer")
    public ResponseEntity<List<TransactionResponse>> transfer(@Valid @RequestBody TransferRequest transferRequest,
                                                             @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(transactionService.transfer(transferRequest, jwt));

    }

    @GetMapping
    public ResponseEntity<PagedTransactionResponse> getTransactions(@PageableDefault(size = 10, sort = "transactedAt",
                                                                                direction = Sort.Direction.DESC) Pageable pageable,
                                                                    @RequestParam FilterSearch transactionType,
                                                                    @AuthenticationPrincipal Jwt jwt){



        return ResponseEntity.ok(transactionService.getTransactions(pageable, transactionType, jwt));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable UUID transactionId, @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(transactionService.getTransactionById(transactionId, jwt));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionResponse>> getTransactionByAccount(@PathVariable UUID accountId,
                                                                       @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(transactionService.getTransactionByAccount(accountId, jwt));
    }

    @GetMapping("/ref/{referenceNumber}")
    public ResponseEntity<TransactionResponse> getTransactionByReferenceNumber(@PathVariable String referenceNumber,
                                                                    @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(transactionService.getTransactionByReferenceNumber(referenceNumber, jwt));
    }


}
