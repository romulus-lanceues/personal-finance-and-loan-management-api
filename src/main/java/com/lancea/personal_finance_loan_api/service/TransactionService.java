package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.dto.request.DepositRequest;
import com.lancea.personal_finance_loan_api.dto.request.TransferRequest;
import com.lancea.personal_finance_loan_api.dto.request.WithdrawRequest;
import com.lancea.personal_finance_loan_api.dto.response.MonthlySummaryResponse;
import com.lancea.personal_finance_loan_api.dto.response.PagedTransactionResponse;
import com.lancea.personal_finance_loan_api.dto.response.TransactionResponse;
import com.lancea.personal_finance_loan_api.entity.Account;
import com.lancea.personal_finance_loan_api.entity.Transaction;
import com.lancea.personal_finance_loan_api.enums.FilterSearch;
import com.lancea.personal_finance_loan_api.enums.TransactionType;
import com.lancea.personal_finance_loan_api.exception.AccountNotFoundException;
import com.lancea.personal_finance_loan_api.exception.BadRequestException;
import com.lancea.personal_finance_loan_api.exception.DuplicateTransactionException;
import com.lancea.personal_finance_loan_api.exception.ResourceNotFoundException;
import com.lancea.personal_finance_loan_api.repository.AccountRepository;
import com.lancea.personal_finance_loan_api.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
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
                .ifPresent( existing -> {
                    throw new DuplicateTransactionException("Duplicate transaction detected", TransactionResponse.of(existing));
                });

        Account account = accountRepository.findByIdAndUserIdAndIsDeletedFalse(depositRequest.accountId(), userId)
                .orElseThrow( () -> new AccountNotFoundException("Account doesn't exist or deleted"));

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

    @Transactional
    public TransactionResponse withdraw(WithdrawRequest withdrawRequest, Jwt jwt){

        UUID userId = UUID.fromString(jwt.getClaim("userId"));

        transactionRepository.findByIdempotencyKey(withdrawRequest.idempotencyKey())
                .ifPresent( transaction -> {
            throw new DuplicateTransactionException("Duplicated transaction detected", TransactionResponse.of(transaction));
        } );

        Account account = accountRepository.findByIdAndUserIdAndIsDeletedFalse(withdrawRequest.accountId(), userId)
                .orElseThrow( ( () -> new AccountNotFoundException("Account doesn't exist or deleted")));

        if(!account.getIsActive()) throw new BadRequestException("Cannot withdraw to a closed account");

        if(account.getBalance().compareTo(withdrawRequest.amount()) < 0){
            throw new BadRequestException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(withdrawRequest.amount()));
        accountRepository.save(account);


        Transaction transaction = Transaction.builder()
                .account(account)
                .referenceNumber(referenceNumberGenerator.generate())
                .type(TransactionType.WITHDRAWAL)
                .amount(withdrawRequest.amount())
                .category(withdrawRequest.category())
                .idempotencyKey(withdrawRequest.idempotencyKey())
                .description(withdrawRequest.idempotencyKey())
                .transactedAt(Instant.now())
                .build();

        return TransactionResponse.of(transactionRepository.save(transaction));

    }

    @Transactional
    public List<TransactionResponse> transfer(TransferRequest transferRequest, Jwt jwt){
        UUID userId = UUID.fromString(jwt.getClaim("userId"));

        transactionRepository.findByIdempotencyKey(transferRequest.idempotencyKey())
                .ifPresent( transaction -> {
                    throw new DuplicateTransactionException("Duplicated transaction detected", TransactionResponse.of(transaction));
                } );

        if(transferRequest.fromAccountId().equals(transferRequest.toAccountId())) throw  new BadRequestException("Cannot transfer to the same account");

        Account fromAccount = accountRepository.findByIdAndUserIdAndIsDeletedFalse(transferRequest.fromAccountId(), userId)
                .orElseThrow( ( () -> new AccountNotFoundException("Account doesn't exist or deleted")));

        Account toAccount = accountRepository.findByIdAndUserIdAndIsDeletedFalse(transferRequest.toAccountId(), userId)
                .orElseThrow( ( () -> new AccountNotFoundException("Account doesn't exist or deleted")));


        if(!fromAccount.getIsActive()) throw new BadRequestException("Source account is closed");
        if(!toAccount.getIsActive()) throw new BadRequestException("Destination account is closed");

        if(fromAccount.getBalance().compareTo(transferRequest.amount()) < 0)  throw new BadRequestException("Insufficient balance");

        fromAccount.setBalance(fromAccount.getBalance().subtract(transferRequest.amount()));
        toAccount.setBalance(toAccount.getBalance().add(transferRequest.amount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        String transferReference = UUID.randomUUID().toString();

        Transaction debit = Transaction.builder()
                .account(fromAccount)
                .referenceNumber(referenceNumberGenerator.generate())
                .type(TransactionType.TRANSFER)
                .amount(transferRequest.amount())
                .category("Transfer Out")
                .idempotencyKey(transferRequest.idempotencyKey())
                .description(transferRequest.description())
                .transactedAt(Instant.now())
                .transferReference(transferReference)
                .build();

        Transaction credit = Transaction.builder()
                .account(toAccount)
                .referenceNumber(referenceNumberGenerator.generate())
                .type(TransactionType.TRANSFER)
                .amount(transferRequest.amount())
                .category("Transfer In")
                .idempotencyKey(null)
                .description(transferRequest.description())
                .transactedAt(Instant.now())
                .transferReference(transferReference)
                .build();

        transactionRepository.save(debit);
        transactionRepository.save(credit);

        return List.of(TransactionResponse.of(debit), TransactionResponse.of(credit));
    }

    public PagedTransactionResponse getTransactions(Pageable pageable, FilterSearch transactionType,
                                                     Jwt jwt){

        UUID userId = UUID.fromString(jwt.getClaim("userId"));

        switch(transactionType) {

            case ALL -> {
                Page<Transaction> allTransactions = transactionRepository.findByAccountUserId(userId, pageable);
                return PagedTransactionResponse.of(allTransactions);
            }

            case DEPOSIT -> {
                Page<Transaction> depositTransactions =  transactionRepository.findByAccountUserIdAndType(userId, TransactionType.DEPOSIT, pageable);
                return PagedTransactionResponse.of(depositTransactions);
            }

            case WITHDRAWAL -> {
                Page<Transaction> withdrawalTransactions =  transactionRepository.findByAccountUserIdAndType(userId, TransactionType.WITHDRAWAL, pageable);
                return PagedTransactionResponse.of(withdrawalTransactions);
            }

            case TRANSFER -> {
                Page<Transaction> transferTransactions =  transactionRepository.findByAccountUserIdAndType(userId, TransactionType.TRANSFER, pageable);
                return PagedTransactionResponse.of(transferTransactions);
            }

            default -> throw new BadRequestException("A proper search filter is required");

        }

    }

    public TransactionResponse getTransactionById(UUID transactionId, Jwt jwt){
        UUID userId = UUID.fromString(jwt.getClaim("userId"));

        Transaction transaction = transactionRepository.findByIdAndAccountUserId(transactionId, userId)
                .orElseThrow( () -> new ResourceNotFoundException("Transaction not found"));

        return TransactionResponse.of(transaction);
    }

    public List<TransactionResponse> getTransactionByAccount(UUID accountId, Jwt jwt){
        UUID userId = UUID.fromString(jwt.getClaim("userId"));

        List<Transaction> transaction = transactionRepository.findByAccountIdAndAccountUserIdAndIsDeletedFalse(accountId, userId);

        return transaction.stream().map(TransactionResponse::of).toList( );

    }

    public TransactionResponse getTransactionByReferenceNumber(String referenceNumber, Jwt jwt){
        UUID userId = UUID.fromString(jwt.getClaim("userId"));

        Transaction transaction = transactionRepository.findByAccountUserIdAndReferenceNumber(userId, referenceNumber)
                .orElseThrow( () -> new ResourceNotFoundException("Transaction not found"));

        return TransactionResponse.of(transaction);
    }

    public List<MonthlySummaryResponse> getMonthlySummary(int year, int month, Jwt jwt){
        UUID userId = UUID.fromString(jwt.getClaim("userId"));

        return  transactionRepository.monthlySummary(userId, year, month).stream()
                .map(MonthlySummaryResponse::of).toList();
    }
}
