package com.lancea.personal_finance_loan_api.repository;

import com.lancea.personal_finance_loan_api.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

}
