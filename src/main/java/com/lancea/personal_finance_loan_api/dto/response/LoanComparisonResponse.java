package com.lancea.personal_finance_loan_api.dto.response;

import java.math.BigDecimal;

public record LoanComparisonResponse(
        LoanDetails loanA,
        LoanDetails loanB,
        BigDecimal interestDifference,
        BigDecimal monthlyPaymentDifference,
        String loanWithLowerCost
) {
}

