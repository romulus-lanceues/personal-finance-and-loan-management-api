package com.lancea.personal_finance_loan_api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record LoanResponse(
        UUID loanId,
        String loadName,
        Instant createdAt
) {
}
