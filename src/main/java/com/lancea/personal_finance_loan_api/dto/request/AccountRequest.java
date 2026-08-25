package com.lancea.personal_finance_loan_api.dto.request;

import com.lancea.personal_finance_loan_api.enums.AccountType;
import com.lancea.personal_finance_loan_api.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccountRequest(

         @NotBlank(message = "Account name is required")
         @Schema( description = "Name of the account to be created", example = "GCash account")
         String accountName,

         @NotNull(message = "Account type required")
         @Schema( description = "Account type of the account",
                 example = "CASH")
         AccountType accountType,

         @Schema( description = "The currency the account will be using",
                    example = "PHP")
         @NotNull(message = "Currency is required")
         Currency currency
) {
}
