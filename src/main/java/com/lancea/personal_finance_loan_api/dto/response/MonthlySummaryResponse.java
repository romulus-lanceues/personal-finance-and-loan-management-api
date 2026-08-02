package com.lancea.personal_finance_loan_api.dto.response;

import com.lancea.personal_finance_loan_api.repository.projection.MonthlySummaryProjection;

import java.math.BigDecimal;

public record MonthlySummaryResponse(
        String category,
        BigDecimal totalAmount,
        Long transactionCount
) {

    public static MonthlySummaryResponse of (MonthlySummaryProjection monthlySummaryProjection){
        return new MonthlySummaryResponse(monthlySummaryProjection.getCategory(),
                monthlySummaryProjection.getTotalAmount(),
                monthlySummaryProjection.getTransactionCount());
    }
}
