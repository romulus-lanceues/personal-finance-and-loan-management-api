package com.lancea.personal_finance_loan_api.repository;

import com.lancea.personal_finance_loan_api.entity.Transaction;
import com.lancea.personal_finance_loan_api.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findByAccountUserId(UUID userId, Pageable pageable);
    Page<Transaction> findByAccountUserIdAndType(UUID userId, TransactionType type, Pageable pageable);

    Optional<Transaction> findByIdAndAccountUserId(UUID id, UUID userId);

    List<Transaction> findByAccountIdAndAccountUserIdAndIsDeletedFalse(UUID accountId, UUID userId);

    Optional<Transaction> findByAccountUserIdAndReferenceNumber(UUID userId, String referenceNumber);
}
