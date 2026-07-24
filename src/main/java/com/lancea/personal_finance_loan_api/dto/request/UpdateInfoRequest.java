package com.lancea.personal_finance_loan_api.dto.request;

import com.lancea.personal_finance_loan_api.validation.annotation.AtLeastOneField;
import jakarta.validation.constraints.Email;
import tools.jackson.databind.annotation.JsonDeserialize;

@AtLeastOneField
public record UpdateInfoRequest(
        @JsonDeserialize(using = BlankToNullStringDeserializer.class)
        String fullName,

        @Email
        @JsonDeserialize(using = BlankToNullStringDeserializer.class)
        String email) {
}
