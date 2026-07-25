package com.lancea.personal_finance_loan_api.dto.request;

import com.lancea.personal_finance_loan_api.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccountUpdateRequest(

        @NotBlank(message = "Account name is required")
        String accountName,

        @NotNull(message = "Account type is required")
        AccountType accountType
) {
}
