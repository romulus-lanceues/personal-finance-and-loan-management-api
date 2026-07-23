package com.lancea.personal_finance_loan_api.validation.annotation;


import com.lancea.personal_finance_loan_api.validation.validator.AtLeastOneFieldValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AtLeastOneFieldValidator.class)
public @interface AtLeastOneField {

    String message() default "At least one field must contain a value";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
