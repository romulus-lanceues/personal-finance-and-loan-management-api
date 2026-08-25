package com.lancea.personal_finance_loan_api.dto.request;

import com.lancea.personal_finance_loan_api.enums.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccountUpdateRequest(

        @NotBlank(message = "Account name is required")
        @Schema(
                description = "Account name specified by the user",
                example = "Savings Account"
        )
        String accountName,

        @NotNull(message = "Account type is required")
        @Schema(
                description = "Account type of the account",
                example = "Available account types (SAVINGS, CHECKING, CASH)"
        )
        AccountType accountType
) {
}
