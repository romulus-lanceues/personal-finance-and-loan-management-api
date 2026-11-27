package com.lancea.personal_finance_loan_api.validation.annotation;

import com.lancea.personal_finance_loan_api.validation.validator.TransactionAmountValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TransactionAmountValidator.class)
public @interface TransactionAmount {

    String message() default "Invalid transaction amount";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
