package com.lancea.personal_finance_loan_api.repository;

import com.lancea.personal_finance_loan_api.entity.LoanSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanScheduleRepository extends JpaRepository<LoanSchedule, UUID> {

    Optional<LoanSchedule> findByLoanIdAndPaymentNumber(UUID loanId, int paymentNumber);

    List<LoanSchedule> findByLoanId(UUID loanId);
    
}
