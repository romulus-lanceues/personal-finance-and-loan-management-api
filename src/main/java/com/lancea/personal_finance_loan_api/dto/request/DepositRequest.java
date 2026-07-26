package com.lancea.personal_finance_loan_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositRequest(
        @NotNull(message = "Account Id required")
        UUID accountId,

        @NotNull(message = "Amount is required")
        BigDecimal amount,

        @NotBlank(message = "Category is required")
        String category,

        String description,

        @NotBlank(message = "Idempotency key required")
        String idempotencyKey
) {
}
