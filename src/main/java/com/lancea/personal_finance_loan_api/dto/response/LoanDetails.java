package com.lancea.personal_finance_loan_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record LoanDetails(
        @Schema(description = "Name of the loan", example = "Car Loan")
        String loanName,

        @Schema(description = "Loan ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID loanId,

        @Schema(description = "Loan annual rate", example = "4.0")
        BigDecimal annualRate,

        @Schema(description = "The total payment amount of the loan with interest", example = "234564")
        BigDecimal amountPayable,

        @Schema(description = "The total interest the user will pay for their loan", example = "4000")
        BigDecimal totalInterest
) {
}
