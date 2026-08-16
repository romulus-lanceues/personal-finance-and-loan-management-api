package com.lancea.personal_finance_loan_api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lancea.personal_finance_loan_api.entity.Transaction;
import com.lancea.personal_finance_loan_api.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
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
) implements AuditableInterface{

    public static TransactionResponse of(Transaction transaction){
        return new TransactionResponse(transaction.getId(),
                transaction.getAccount().getId(),
                (transaction.getLoan() == null) ? null : transaction.getLoan().getId(),
                transaction.getReferenceNumber(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getCategory(),
                transaction.getDescription(),
                transaction.getTransactedAt(),
                transaction.getCreatedAt());
    }

    @JsonIgnore
    @Override
    public UUID getResultObjectId() {
        return this.id;
    }

    @Override
    public Map<String, Object> generateAuditDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("transactionId", this.id);
        details.put("accountId", this.accountId);
        details.put("referenceNumber", this.referenceNumber);
        details.put("type", this.type);
        details.put("amount", this.amount);
        details.put("category", this.category);

        return details;
    }
}
