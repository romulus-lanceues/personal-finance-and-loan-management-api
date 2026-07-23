package com.lancea.personal_finance_loan_api.dto.request;

import com.lancea.personal_finance_loan_api.validation.annotation.AtLeastOneField;
import jakarta.validation.constraints.Email;

@AtLeastOneField
public record UpdateInfoRequest(
        
        String fullName,

        @Email
        String email) {
}
