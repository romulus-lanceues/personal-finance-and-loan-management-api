package com.lancea.personal_finance_loan_api.validation.validator;

import com.lancea.personal_finance_loan_api.dto.request.UpdateInfoRequest;
import com.lancea.personal_finance_loan_api.validation.annotation.AtLeastOneField;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AtLeastOneFieldValidator implements ConstraintValidator<AtLeastOneField, UpdateInfoRequest> {

    @Override
    public boolean isValid(UpdateInfoRequest updateInfoRequest, ConstraintValidatorContext constraint){


        if(updateInfoRequest.email() == null && updateInfoRequest.fullName() == null){

            constraint.disableDefaultConstraintViolation();
            constraint.buildConstraintViolationWithTemplate("Both fields are empty")
                    .addPropertyNode("email & fullName")
                    .addConstraintViolation();

            return false;
        }

        return true;
    }
}
