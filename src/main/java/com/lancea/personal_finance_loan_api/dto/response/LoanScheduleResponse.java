package com.lancea.personal_finance_loan_api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lancea.personal_finance_loan_api.entity.LoanSchedule;
import com.lancea.personal_finance_loan_api.enums.LoanScheduleStatus;
import com.lancea.personal_finance_loan_api.repository.LoanRepository;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record LoanScheduleResponse(
        UUID id,
        UUID loanId,
        int paymentNumber,
        BigDecimal paymentAmount,
        BigDecimal principalPortion,
        BigDecimal interestPortion,
        BigDecimal remainingBalance,
        LocalDate dueDate,
        LoanScheduleStatus loanScheduleStatus
)  {

    public static LoanScheduleResponse of(LoanSchedule schedule){
        return new LoanScheduleResponse(
                schedule.getId(),
                schedule.getLoan().getId(),
                schedule.getPaymentNumber(),
                schedule.getPaymentAmount(),
                schedule.getPrincipalPortion(),
                schedule.getInterestPortion(),
                schedule.getRemainingBalance(),
                schedule.getDueDate(),
                schedule.getStatus());
    }
}
