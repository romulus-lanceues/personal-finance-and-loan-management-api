package com.lancea.personal_finance_loan_api.dto.request;

import com.lancea.personal_finance_loan_api.enums.AccountType;
import com.lancea.personal_finance_loan_api.enums.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccountRequest(

         @NotBlank(message = "Account name is required")
         String accountName,

         @NotNull(message = "Account type required")
         AccountType accountType,

         @NotNull(message = "Currency is required")
         Currency currency
) {
}
