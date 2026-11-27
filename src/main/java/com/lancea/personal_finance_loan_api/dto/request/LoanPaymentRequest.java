package com.lancea.personal_finance_loan_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.CodePointLength;

public record LoanPaymentRequest(
        @NotBlank(message = "Idempotency key is required")
        @CodePointLength(max = 255, message = "Idempotency key must be at most 255 characters")
        String idempotencyKey
) {
}
