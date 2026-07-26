package com.lancea.personal_finance_loan_api.dto.response;

import java.math.BigDecimal;

public record MonthlySummaryResponse(
        String category,
        BigDecimal totalAmount,
        int transactionCount
) {
}
