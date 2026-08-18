package com.lancea.personal_finance_loan_api.dto.response;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record LoanPaymentResponse(
        LoanResponse loanResponse,
        LoanScheduleResponse loanScheduleResponse,
        TransactionResponse transactionResponse
) implements AuditableInterface {


    @Override
    public UUID getResultEntityId() {
        return loanResponse.loanId();
    }

    @Override
    public Map<String, Object> generateAuditDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("loanId", loanResponse.loanId());
        details.put("loanName", loanResponse.loanName());
        details.put("paymentNumber", loanScheduleResponse.paymentNumber());
        details.put("amountPaid", transactionResponse.amount());
        details.put("accountDebited", transactionResponse.accountId());
        details.put("remainingBalance", loanScheduleResponse.remainingBalance());
        details.put("loanStatusAfter", loanResponse.status());
        return details;
    }
}
