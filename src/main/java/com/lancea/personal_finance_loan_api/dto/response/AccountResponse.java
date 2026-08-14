package com.lancea.personal_finance_loan_api.dto.response;

import com.lancea.personal_finance_loan_api.entity.Account;
import com.lancea.personal_finance_loan_api.enums.AccountType;
import com.lancea.personal_finance_loan_api.enums.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record AccountResponse  (
        UUID id, String accountName, AccountType accountType,
        BigDecimal balance, Currency currency,
        boolean isActive, Instant createdAt,
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

    @Override
    public UUID getResultObjectId(){
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
