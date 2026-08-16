package com.lancea.personal_finance_loan_api.controller;

import com.lancea.personal_finance_loan_api.dto.request.AccountRequest;
import com.lancea.personal_finance_loan_api.dto.request.AccountUpdateRequest;
import com.lancea.personal_finance_loan_api.dto.response.AccountResponse;
import com.lancea.personal_finance_loan_api.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
    @RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@AuthenticationPrincipal Jwt jwt,
                                                         @Valid @RequestBody AccountRequest accountRequest){


        AccountResponse response = accountService.createAccount(jwt, accountRequest);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(accountService.getAllAccounts(jwt));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable UUID accountId,
                                                          @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(accountService.getAccountById(accountId, jwt ));

    }

    @PutMapping("/{accountId}")
    public ResponseEntity<AccountResponse> updateAccount(@PathVariable UUID accountId,
                                                         @Valid @RequestBody AccountUpdateRequest updateRequest,
                                                         @AuthenticationPrincipal Jwt jwt){

        return ResponseEntity.ok(accountService.updateAccount(accountId, updateRequest, jwt));

    }

    @PatchMapping("/{accountId}/close")
    public ResponseEntity<AccountResponse> closeAccount(@PathVariable UUID accountId,
                                                        @AuthenticationPrincipal Jwt jwt)  {

        return ResponseEntity.ok(accountService.closeAccount(accountId, jwt));
    }


    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(@PathVariable UUID accountId,
                                              @AuthenticationPrincipal Jwt jwt) {

        accountService.deleteAccount(accountId, jwt);
        return ResponseEntity.noContent().build();
    }



}

