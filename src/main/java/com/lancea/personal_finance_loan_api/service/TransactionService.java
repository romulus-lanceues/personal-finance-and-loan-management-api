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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ReferenceNumberGenerator referenceNumberGenerator;
    private final CacheManager cacheManager;

    @PersistenceContext
    private EntityManager entityManager;

    @Auditable(action = "DEPOSIT", entityType = "TRANSACTION")
    @Transactional
    public TransactionResponse deposit(DepositRequest depositRequest, Jwt jwt){

        BigDecimal depositAmount = validateAmount(depositRequest.amount());
        UUID userId = UserUtility.getUserId(jwt);

        Account account = lockAccount(depositRequest.accountId(), userId);

        Optional<Transaction> transaction = transactionRepository
                .findByIdempotencyKeyAndAccountIdAndAccountUserId(
                        depositRequest.idempotencyKey(),
                        depositRequest.accountId(),
                        userId
                );

        if (transaction.isPresent()) {
            Transaction existingTransaction = transaction.get();
            if (matchesRequest(existingTransaction, TransactionType.DEPOSIT, depositAmount, depositRequest.description())
                    && Objects.equals(existingTransaction.getCategory(), depositRequest.category())) {
                return TransactionResponse.of(existingTransaction);
            }

            throw new DuplicateTransactionException("Idempotency key already used for a different request.");
        }

        if(!account.getIsActive()) throw new BadRequestException("Cannot deposit to a closed account");

        account.setBalance(validateBalance(account.getBalance().add(depositAmount)));
        accountRepository.save(account);

        Transaction createdTransaction = Transaction.builder()
                .account(account)
                .referenceNumber(referenceNumberGenerator.generate())
                .type(TransactionType.DEPOSIT)
                .amount(depositAmount)
                .category(depositRequest.category())
                .idempotencyKey(depositRequest.idempotencyKey())
                .description(depositRequest.description())
                .transactedAt(Instant.now())
                .build();

        return saveAndRespond(createdTransaction, userId);

    }


    @Auditable(action = "WITHDRAWAL", entityType = "TRANSACTION")
    @Transactional
    public TransactionResponse withdraw(WithdrawRequest withdrawRequest, Jwt jwt){

        BigDecimal amount = validateAmount(withdrawRequest.amount());
        UUID userId = UserUtility.getUserId(jwt);

        Account account = lockAccount(withdrawRequest.accountId(), userId);
        Optional<Transaction> previous = transactionRepository.findByIdempotencyKeyAndAccountIdAndAccountUserId(
                withdrawRequest.idempotencyKey(), account.getId(), userId);

        if (previous.isPresent()) {
            Transaction existing = previous.get();
            if (matchesRequest(existing, TransactionType.WITHDRAWAL, amount, withdrawRequest.description())
                    && Objects.equals(existing.getCategory(), withdrawRequest.category())) {
                return TransactionResponse.of(existing);
            }
            throw new DuplicateTransactionException("Idempotency key already used for a different request.");
        }

        if(!account.getIsActive()) throw new BadRequestException("Cannot withdraw from a closed account");

        if(account.getBalance().compareTo(amount) < 0){
            throw new BadRequestException("Insufficient balance");
        }

        account.setBalance(validateBalance(account.getBalance().subtract(amount)));
        accountRepository.save(account);


        Transaction transaction = Transaction.builder()
                .account(account)
                .referenceNumber(referenceNumberGenerator.generate())
                .type(TransactionType.WITHDRAWAL)
                .amount(amount)
                .category(withdrawRequest.category())
                .idempotencyKey(withdrawRequest.idempotencyKey())
                .description(withdrawRequest.description())
                .transactedAt(Instant.now())
                .build();

        return saveAndRespond(transaction, userId);

    }

    @Auditable(action = "TRANSFER", entityType = "TRANSACTION")
    @Transactional
    public List<TransactionResponse> transfer(TransferRequest transferRequest, Jwt jwt){

        BigDecimal amount = validateAmount(transferRequest.amount());
        UUID userId = UserUtility.getUserId(jwt);
        UUID fromAccountId = transferRequest.fromAccountId();
        UUID toAccountId = transferRequest.toAccountId();

        if (fromAccountId.equals(toAccountId)) {
            throw new BadRequestException("Cannot transfer to the same account");
        }

        // Lock both accounts in the same order for transfers in either direction.
        boolean sourceFirst = fromAccountId.compareTo(toAccountId) < 0;
        Account firstAccount = lockAccount(sourceFirst ? fromAccountId : toAccountId, userId);
        Account secondAccount = lockAccount(sourceFirst ? toAccountId : fromAccountId, userId);
        Account fromAccount = sourceFirst ? firstAccount : secondAccount;
        Account toAccount = sourceFirst ? secondAccount : firstAccount;

        Optional<Transaction> previous = transactionRepository.findByIdempotencyKeyAndAccountIdAndAccountUserId(
                transferRequest.idempotencyKey(), fromAccountId, userId);
        if (previous.isPresent()) {
            Transaction debit = previous.get();
            if (matchesRequest(debit, TransactionType.TRANSFER, amount, transferRequest.description())
                    && debit.getTransferReference() != null) {
                Optional<Transaction> credit = transactionRepository
                        .findByTransferReferenceAndAccountIdAndAccountUserIdAndIdempotencyKeyIsNull(
                                debit.getTransferReference(), toAccountId, userId);
                if (credit.isPresent()) {
                    return List.of(TransactionResponse.of(debit), TransactionResponse.of(credit.get()));
                }
            }
            throw new DuplicateTransactionException("Idempotency key already used for a different request.");
        }

        if(!fromAccount.getIsActive()) throw new BadRequestException("Source account is closed");
        if(!toAccount.getIsActive()) throw new BadRequestException("Destination account is closed");

        if(fromAccount.getBalance().compareTo(amount) < 0)  throw new BadRequestException("Insufficient balance");

        BigDecimal sourceBalance = validateBalance(fromAccount.getBalance().subtract(amount));
        BigDecimal destinationBalance = validateBalance(toAccount.getBalance().add(amount));
        fromAccount.setBalance(sourceBalance);
        toAccount.setBalance(destinationBalance);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        String transferReference = UUID.randomUUID().toString();

        Transaction debit = Transaction.builder()
                .account(fromAccount)
                .referenceNumber(referenceNumberGenerator.generate())
                .type(TransactionType.TRANSFER)
                .amount(amount)
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
                .amount(amount)
                .category("Transfer In")
                .idempotencyKey(null)
                .description(transferRequest.description())
                .transactedAt(Instant.now())
                .transferReference(transferReference)
                .build();

        transactionRepository.save(debit);
        transactionRepository.saveAndFlush(credit);
        entityManager.refresh(debit);
        entityManager.refresh(credit);

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


    private BigDecimal validateAmount(BigDecimal requestedAmount) {
        BigDecimal amount;
        try {
            amount = requestedAmount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new BadRequestException("Amount must fit within two decimal places.");
        }

        if (amount.signum() <= 0 || amount.precision() - amount.scale() > 15) {
            throw new BadRequestException("Amount must be positive and fit numeric(19,4).");
        }
        return amount;
    }

    private BigDecimal validateBalance(BigDecimal balance) {
        if (balance.precision() - balance.scale() > 15) {
            throw new BadRequestException("This transaction would exceed the maximum account balance.");
        }
        return balance;
    }

    private Account lockAccount(UUID accountId, UUID userId) {
        return accountRepository.findForUpdateByIdAndUserIdAndIsDeletedFalse(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account doesn't exist or has been deleted"));
    }

    private boolean matchesRequest(Transaction transaction, TransactionType type, BigDecimal amount,
                                   String description) {
        return transaction.getType() == type
                && transaction.getAmount().compareTo(amount) == 0
                && Objects.equals(transaction.getDescription(), description);
    }

    private TransactionResponse saveAndRespond(Transaction transaction, UUID userId) {
        Transaction saved = transactionRepository.saveAndFlush(transaction);
        entityManager.refresh(saved);
        evictSpendingSummaryCache(userId, saved.getTransactedAt());
        return TransactionResponse.of(saved);
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
