package com.lancea.personal_finance_loan_api.dto.request;

import com.lancea.personal_finance_loan_api.validation.annotation.TransactionAmount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.CodePointLength;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(

        @NotNull(message = "Source account ID is required")
        UUID fromAccountId,

        @NotNull(message = "Destination account ID is required")
        UUID toAccountId,

        @NotNull(message = "Amount is required")
        @TransactionAmount
        BigDecimal amount,

        String description,

        @NotBlank(message = "Idempotency key is required")
        @CodePointLength(max = 255, message = "Idempotency key must be at most 255 characters")
        String idempotencyKey

) {
}
