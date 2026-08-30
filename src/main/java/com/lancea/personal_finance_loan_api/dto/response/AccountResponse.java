package com.lancea.personal_finance_loan_api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lancea.personal_finance_loan_api.entity.Account;
import com.lancea.personal_finance_loan_api.enums.AccountType;
import com.lancea.personal_finance_loan_api.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record AccountResponse  (
        @Schema(description = "Account ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Account name", example = "My retirement savings")
        String accountName,

        @Schema(description = "Account Type", example = "CASH")
        AccountType accountType,

        @Schema(description = "The balance of the account", example = "100000")
        BigDecimal balance, Currency currency,

        @Schema(description = "The status of the account")
        boolean isActive,

        @Schema(description = "Instance date when the account was created")
        Instant createdAt,

        @Schema(description = "Instance date when the account was last updated ")
        Instant updatedAt ) implements AuditableInterface{

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

    @JsonIgnore
    @Override
    public UUID getResultEntityId(){
        return this.id;
    }

    @Override
    public Map<String, Object> generateAuditDetails() {
        HashMap<String, Object> details = new LinkedHashMap<>();

        details.put("accountId", this.id);
        details.put("accountName", this.accountName);
        details.put("accountType", this.accountType);
        details.put("balance", this.balance);
        details.put("currency", this.currency);
        details.put("isActive", this.isActive);

        return details;
    }
}
