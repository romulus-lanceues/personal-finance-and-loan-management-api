package com.lancea.personal_finance_loan_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoanPaymentRequest(
        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey
) {
}
