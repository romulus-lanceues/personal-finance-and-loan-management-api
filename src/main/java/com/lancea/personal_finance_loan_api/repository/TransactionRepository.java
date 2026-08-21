package com.lancea.personal_finance_loan_api.repository;

import com.lancea.personal_finance_loan_api.entity.Transaction;
import com.lancea.personal_finance_loan_api.enums.TransactionType;
import com.lancea.personal_finance_loan_api.repository.projection.MonthlySummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    @Query("""
            SELECT t.category AS category,
                SUM(t.amount) AS totalAmount,
                COUNT(t) AS transactionCount
            FROM Transaction t
            WHERE t.account.user.id = :userId
                AND t.type IN ('WITHDRAWAL', 'TRANSFER', 'LOAN_PAYMENT', 'DEPOSIT')
                AND EXTRACT(YEAR FROM t.transactedAt) = :year
                AND EXTRACT(MONTH FROM t.transactedAt) =:month
                AND t.isDeleted = false
            GROUP BY t.category
            ORDER BY totalAmount DESC
    """)
    List<MonthlySummaryProjection> monthlySummary(UUID userId, int year, int month);
}
