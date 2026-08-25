package com.lancea.personal_finance_loan_api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The currency the account is using")
public enum Currency {
    PHP,
    USD,
    JPY
}
