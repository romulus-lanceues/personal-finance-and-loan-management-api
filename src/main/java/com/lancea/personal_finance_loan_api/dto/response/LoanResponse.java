package com.lancea.personal_finance_loan_api.dto.response;

import com.lancea.personal_finance_loan_api.entity.Loan;

import java.time.Instant;
import java.util.UUID;

public record LoanResponse(
        UUID loanId,
        String loadName,
        Instant createdAt
) {

    public static LoanResponse of(Loan loan){
        return new LoanResponse(loan.getId(),
                loan.getLoanName(),
                loan.getCreatedAt());
    }
}
