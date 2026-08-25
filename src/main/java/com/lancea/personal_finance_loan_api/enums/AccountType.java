package com.lancea.personal_finance_loan_api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Species the type of accounts the user have")
public enum AccountType {
    SAVINGS,
    CHECKING,
    CASH
}
