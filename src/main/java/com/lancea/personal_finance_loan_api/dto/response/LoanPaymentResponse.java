package com.lancea.personal_finance_loan_api.dto.response;

public record LoanPaymentResponse(
        LoanResponse loanResponse,
        LoanScheduleResponse loanScheduleResponse,
        TransactionResponse transactionResponse
) {
}
