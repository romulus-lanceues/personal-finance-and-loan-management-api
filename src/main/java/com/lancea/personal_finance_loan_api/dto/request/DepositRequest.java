package com.lancea.personal_finance_loan_api.dto.request;

import com.lancea.personal_finance_loan_api.validation.annotation.TransactionAmount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.CodePointLength;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositRequest(
        @NotNull(message = "Account Id required")
        UUID accountId,

        @NotNull(message = "Amount is required")
        @TransactionAmount
        BigDecimal amount,

        @NotBlank(message = "Category is required")
        String category,

        String description,

        @NotBlank(message = "Idempotency key required")
        @CodePointLength(max = 255, message = "Idempotency key must be at most 255 characters")
        String idempotencyKey
) {
}
