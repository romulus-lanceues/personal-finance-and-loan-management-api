package com.lancea.personal_finance_loan_api.validation.validator;

import com.lancea.personal_finance_loan_api.validation.annotation.TransactionAmount;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class TransactionAmountValidator implements ConstraintValidator<TransactionAmount, BigDecimal> {

    public static final int AMOUNT_SCALE = 2;
    private static final int MAX_INTEGER_DIGITS = 15;

    @Override
    public boolean isValid(BigDecimal amount, ConstraintValidatorContext context) {
        // @NotNull handles missing values in request validation.
        String error = amount == null ? null : validationError(amount);
        if (error == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(error).addConstraintViolation();
        return false;
    }

    public static String validationError(BigDecimal amount) {
        if (amount == null) {
            return "Amount is required";
        }
        if (amount.signum() <= 0) {
            return "Amount must be greater than zero";
        }
        if ((long) amount.precision() - amount.scale() > MAX_INTEGER_DIGITS) {
            return "Amount must have at most 15 digits before the decimal point.";
        }
        if (amount.stripTrailingZeros().scale() > AMOUNT_SCALE) {
            return "Amount must fit within two decimal places.";
        }
        return null;
    }
}
