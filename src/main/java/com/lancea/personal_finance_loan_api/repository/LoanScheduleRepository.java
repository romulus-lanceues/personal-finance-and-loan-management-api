package com.lancea.personal_finance_loan_api.repository;

import com.lancea.personal_finance_loan_api.entity.LoanSchedule;
import com.lancea.personal_finance_loan_api.enums.LoanScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanScheduleRepository extends JpaRepository<LoanSchedule, UUID> {

    Optional<LoanSchedule> findByLoanIdAndPaymentNumber(UUID loanId, int paymentNumber);

    List<LoanSchedule> findByLoanId(UUID loanId);

    boolean existsByLoanIdAndPaymentNumberLessThanAndStatus(UUID loanId, int paymentNumber, LoanScheduleStatus loanScheduleStatus);

    boolean existsByLoanIdAndPaymentNumberGreaterThanAndStatus(UUID loanId, int paymentNumber, LoanScheduleStatus loanScheduleStatus);
}