package com.lancea.personal_finance_loan_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record LoanComparisonResponse(
        @Schema(description = "The loan details of the first loan that was compared")
        LoanDetails loanA,

        @Schema(description = "The loan details of the second loan that was compared")
        LoanDetails loanB,

        @Schema(description = "The monthly interest difference between the two loans")
        BigDecimal interestDifference,

        @Schema(description = "The monthly payment difference between the two loans")
        BigDecimal monthlyPaymentDifference,

        @Schema(description = "The name of the loan which cost less", example = "Car Loan")
        String loanWithLowerCost
) {
}

