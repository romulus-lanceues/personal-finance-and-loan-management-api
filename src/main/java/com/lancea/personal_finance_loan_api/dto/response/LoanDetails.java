package com.lancea.personal_finance_loan_api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record LoanDetails(
        String loanName,
        UUID loanId,
        BigDecimal annualRate,
        BigDecimal amountPayable,
        BigDecimal totalInterest
) {
}
