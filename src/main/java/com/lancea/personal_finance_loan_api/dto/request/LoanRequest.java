package com.lancea.personal_finance_loan_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LoanRequest(

        @NotNull(message = "Account ID must not be null")
        UUID accountId,

        @NotBlank(message = "Loan name must not be blank")
        String loanName,

        @NotNull(message = "Principal shouldn't be blank")
        @Positive(message = "Principal amount must be positive")
        BigDecimal principal,

        @NotNull(message = "Annual rate shouldn't be blank")
        BigDecimal annualRate,

        @NotNull(message = "Term months shouldn't be blank")
        @Positive(message = "Term months must be valid")
        int termMonths,

        @NotNull(message = "Disbursed date shouldn't be blank")
        LocalDate disbursedAt) {
}
