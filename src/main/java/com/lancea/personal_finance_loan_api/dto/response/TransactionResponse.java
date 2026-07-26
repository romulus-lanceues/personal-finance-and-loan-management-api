package com.lancea.personal_finance_loan_api.dto.response;

import com.lancea.personal_finance_loan_api.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID accountId,
        UUID loanId,
        String referenceNumber,
        TransactionType type,
        BigDecimal amount,
        String category,
        String description,
        Instant transactedAt,
        Instant createdAt
) {
}
