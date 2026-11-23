package com.lancea.personal_finance_loan_api.enums;

import com.lancea.personal_finance_loan_api.exception.BadRequestException;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The currency the account is using")
public enum Currency {
    PHP,
    // Retain the legacy values for accounts already stored in the database.
    USD,
    JPY;

    public static void requireSupported(Currency currency) {
        if (currency == null) {
            throw new BadRequestException("Currency is required");
        }
        if (currency != PHP) {
            throw new BadRequestException("Only PHP is currently supported");
        }
    }
}
