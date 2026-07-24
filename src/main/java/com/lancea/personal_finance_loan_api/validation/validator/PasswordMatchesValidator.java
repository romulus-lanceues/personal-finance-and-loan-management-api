package com.lancea.personal_finance_loan_api.validation.validator;

import com.lancea.personal_finance_loan_api.dto.request.AuthenticationRequest;
import com.lancea.personal_finance_loan_api.validation.annotation.PasswordMatches;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, AuthenticationRequest> {

    @Override
    public boolean isValid(AuthenticationRequest authenticationRequest, ConstraintValidatorContext context){
        if(authenticationRequest.password() == null || authenticationRequest.confirmPassword() == null) return false;

        boolean matches =  authenticationRequest.password().equals(authenticationRequest.confirmPassword());

        if(!matches){
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Passwords do not match")
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
        }

        return matches;

    }
}
