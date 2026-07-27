package com.lancea.personal_finance_loan_api.dto.response;

import com.lancea.personal_finance_loan_api.entity.Account;
import com.lancea.personal_finance_loan_api.enums.AccountType;
import com.lancea.personal_finance_loan_api.enums.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountName,
        AccountType accountType,
        BigDecimal balance,
        Currency currency,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {

    public static AccountResponse of(Account account){
        return new AccountResponse(
                account.getId(),
                account.getAccountName(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency(),
                account.getIsActive(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
