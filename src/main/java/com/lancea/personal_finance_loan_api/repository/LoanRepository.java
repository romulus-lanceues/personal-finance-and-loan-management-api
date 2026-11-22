package com.lancea.personal_finance_loan_api.repository;

import com.lancea.personal_finance_loan_api.entity.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    Page<Loan> findByUserIdAndIsDeletedIsFalse(UUID userId, Pageable pageable);

    Optional<Loan> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    @Query("""
            SELECT l.account.id
            FROM Loan l
            WHERE l.id = :loanId AND l.user.id = :userId AND l.isDeleted = false
            """)
    Optional<UUID> findFundingAccountId(UUID loanId, UUID userId);

    boolean existsByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);


}
