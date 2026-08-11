package com.lancea.personal_finance_loan_api.repository;

import com.lancea.personal_finance_loan_api.entity.Loan;
import com.lancea.personal_finance_loan_api.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    Page<Loan> findByUserIdAndIsDeletedIsFalse(UUID userId, Pageable pageable);

    Optional<Loan> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    boolean existsByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);


}
