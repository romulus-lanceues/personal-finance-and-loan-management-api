package com.lancea.personal_finance_loan_api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Specifies the current status of the user's loan")
public enum LoanStatus {
    ACTIVE,
    PAID_OFF,
    DEFAULTED
}
