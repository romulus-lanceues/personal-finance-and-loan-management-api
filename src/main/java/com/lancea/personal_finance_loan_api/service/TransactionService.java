package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.aspect.Auditable;
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
import com.lancea.personal_finance_loan_api.exception.BadRequestException;
import com.lancea.personal_finance_loan_api.exception.DuplicateTransactionException;
import com.lancea.personal_finance_loan_api.exception.ResourceNotFoundException;
import com.lancea.personal_finance_loan_api.repository.AccountRepository;
import com.lancea.personal_finance_loan_api.repository.TransactionRepository;
import com.lancea.personal_finance_loan_api.utility.UserUtility;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ReferenceNumberGenerator referenceNumberGenerator;
    private final CacheManager cacheManager;

    @Auditable(action = "DEPOSIT", entityType = "TRANSACTION")
    @Transactional
    public TransactionResponse deposit(DepositRequest depositRequest, Jwt jwt){

        UUID userId = UserUtility.getUserId(jwt);

        transactionRepository.findByIdempotencyKey(depositRequest.idempotencyKey())
                .ifPresent( existing -> {
                    throw new DuplicateTransactionException("Duplicate transaction detected", TransactionResponse.of(existing));
                });

        Account account = accountRepository.findByIdAndUserIdAndIsDeletedFalse(depositRequest.accountId(), userId)
                .orElseThrow( () -> new ResourceNotFoundException("Account doesn't exist or has been deleted"));

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

        evictSpendingSummaryCache(userId, transaction.getTransactedAt());

        return TransactionResponse.of(transactionRepository.save(transaction));

    }



    @Auditable(action = "WITHDRAWAL", entityType = "TRANSACTION")
    @Transactional
    public TransactionResponse withdraw(WithdrawRequest withdrawRequest, Jwt jwt){

        UUID userId = UserUtility.getUserId(jwt);

        transactionRepository.findByIdempotencyKey(withdrawRequest.idempotencyKey())
                .ifPresent( transaction -> {
            throw new DuplicateTransactionException("Duplicated transaction detected", TransactionResponse.of(transaction));
        } );

        Account account = accountRepository.findByIdAndUserIdAndIsDeletedFalse(withdrawRequest.accountId(), userId)
                .orElseThrow( ( () -> new ResourceNotFoundException("Account doesn't exist or has been deleted")));

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

        evictSpendingSummaryCache(userId, transaction.getTransactedAt());

        return TransactionResponse.of(transactionRepository.save(transaction));

    }

    @Auditable(action = "TRANSFER", entityType = "TRANSACTION")
    @Transactional
    public List<TransactionResponse> transfer(TransferRequest transferRequest, Jwt jwt){

        UUID userId = UserUtility.getUserId(jwt);

        transactionRepository.findByIdempotencyKey(transferRequest.idempotencyKey())
                .ifPresent( transaction -> {
                    throw new DuplicateTransactionException("Duplicated transaction detected", TransactionResponse.of(transaction));
                } );

        if(transferRequest.fromAccountId().equals(transferRequest.toAccountId())) throw  new BadRequestException("Cannot transfer to the same account");

        Account fromAccount = accountRepository.findByIdAndUserIdAndIsDeletedFalse(transferRequest.fromAccountId(), userId)
                .orElseThrow( ( () -> new ResourceNotFoundException("Account doesn't exist or has been deleted")));

        Account toAccount = accountRepository.findByIdAndUserIdAndIsDeletedFalse(transferRequest.toAccountId(), userId)
                .orElseThrow( ( () -> new ResourceNotFoundException("Account doesn't exist or has been deleted")));


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

        evictSpendingSummaryCache(userId, debit.getTransactedAt());

        return List.of(TransactionResponse.of(debit), TransactionResponse.of(credit));
    }


    public PagedTransactionResponse getTransactions(Pageable pageable, FilterSearch transactionType,
                                                     Jwt jwt){

        UUID userId = UserUtility.getUserId(jwt);

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
        UUID userId = UserUtility.getUserId(jwt);

        Transaction transaction = transactionRepository.findByIdAndAccountUserId(transactionId, userId)
                .orElseThrow( () -> new ResourceNotFoundException("Transaction not found"));

        return TransactionResponse.of(transaction);
    }

    public List<TransactionResponse> getTransactionByAccount(UUID accountId, Jwt jwt){
        UUID userId = UserUtility.getUserId(jwt);

        List<Transaction> transaction = transactionRepository.findByAccountIdAndAccountUserIdAndIsDeletedFalse(accountId, userId);

        return transaction.stream().map(TransactionResponse::of).toList( );

    }

    public TransactionResponse getTransactionByReferenceNumber(String referenceNumber, Jwt jwt){
        UUID userId = UserUtility.getUserId(jwt);

        Transaction transaction = transactionRepository.findByAccountUserIdAndReferenceNumber(userId, referenceNumber)
                .orElseThrow( () -> new ResourceNotFoundException("Transaction not found"));

        return TransactionResponse.of(transaction);
    }

    @Cacheable(cacheNames = "spending-summary", keyGenerator = "customKeyGenerator")
    public List<MonthlySummaryResponse> getMonthlySummary(int year, int month, Jwt jwt){
        UUID userId = UserUtility.getUserId(jwt);

        return  transactionRepository.monthlySummary(userId, year, month).stream()
                .map(MonthlySummaryResponse::of).collect(Collectors.toList());
    }


    private void evictSpendingSummaryCache(UUID userId, Instant transactedAt){
        LocalDate date = transactedAt.atZone(ZoneOffset.UTC).toLocalDate();
        String key = userId + ":" + date.getYear() + ":" + date.getMonthValue();

        Cache cache = cacheManager.getCache("spending-summary");

        if (cache != null){
            cache.evict(key);
        }
    }
}
