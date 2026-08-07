package com.lancea.personal_finance_loan_api.dto.response;

import com.lancea.personal_finance_loan_api.entity.LoanSchedule;
import com.lancea.personal_finance_loan_api.enums.LoanScheduleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LoanScheduleResponse(
        UUID id,
        int paymentNumber,
        BigDecimal paymentAmount,
        BigDecimal principalPortion,
        BigDecimal interestPortion,
        BigDecimal remainingBalance,
        LocalDate dueDate,
        LoanScheduleStatus loanStatus
) {

    public static LoanScheduleResponse of(LoanSchedule schedule){
        return new LoanScheduleResponse(schedule.getId(),
                schedule.getPaymentNumber(),
                schedule.getPaymentAmount(),
                schedule.getPrincipalPortion(),
                schedule.getInterestPortion(),
                schedule.getRemainingBalance(),
                schedule.getDueDate(),
                schedule.getStatus());
    }
}
