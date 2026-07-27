package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.dto.request.DepositRequest;
import com.lancea.personal_finance_loan_api.dto.response.TransactionResponse;
import com.lancea.personal_finance_loan_api.entity.Account;
import com.lancea.personal_finance_loan_api.entity.Transaction;
import com.lancea.personal_finance_loan_api.enums.TransactionType;
import com.lancea.personal_finance_loan_api.exception.AccountNotFoundException;
import com.lancea.personal_finance_loan_api.exception.BadRequestException;
import com.lancea.personal_finance_loan_api.exception.DuplicateTransactionException;
import com.lancea.personal_finance_loan_api.repository.AccountRepository;
import com.lancea.personal_finance_loan_api.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ReferenceNumberGenerator referenceNumberGenerator;

    @Transactional
    public TransactionResponse deposit(DepositRequest depositRequest, Jwt jwt){

        UUID userId = UUID.fromString(jwt.getClaim("userId"));

        transactionRepository.findByIdempotencyKey(depositRequest.idempotencyKey())
                .ifPresent( existing -> { throw new DuplicateTransactionException("Duplicate transaction detected", TransactionResponse.of(existing));
                });

        Account account = accountRepository.findByIdAndUserIdAndIsDeletedFalse(depositRequest.accountId(), userId).orElseThrow( () -> new AccountNotFoundException("Account doesn't exist or deleted"));

        if(!account.getIsActive()) throw new BadRequestException("Cannot deposit to a closed account");

        account.setBalance(account.getBalance().add(depositRequest.amount()));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .account(account)
                .referenceNumber(referenceNumberGenerator.generate())
                .type(TransactionType.DEPOSIT)
                .amount(depositRequest.amount())
                .category(depositRequest.category())
                .idempotencyKey(depositRequest.idempotencyKey())
                .description(depositRequest.idempotencyKey())
                .transactedAt(Instant.now())
                .build();


        return TransactionResponse.of(transactionRepository.save(transaction));

    }
}
