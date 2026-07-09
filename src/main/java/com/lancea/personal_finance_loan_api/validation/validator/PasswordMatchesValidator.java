package com.lancea.personal_finance_loan_api.validation.validator;

import com.lancea.personal_finance_loan_api.dto.request.RegisterRequest;
import com.lancea.personal_finance_loan_api.validation.annotation.PasswordMatches;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest registerRequest, ConstraintValidatorContext ctx){

        if(registerRequest.password() == null || registerRequest.confirmPassword() == null) return false;

        return registerRequest.password().equals(registerRequest.confirmPassword());

    }
}
