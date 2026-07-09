package com.lancea.personal_finance_loan_api.dto.request;

import com.lancea.personal_finance_loan_api.validation.annotation.PasswordMatches;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@PasswordMatches
public record RegisterRequest(
        @NotBlank(message = "Name must not be blank")
        @Size(min = 3, message = "Full name must be 3 characters long")
        String fullName,

        @NotBlank(message = "Email shouldn't be blank")
        @Email(message = "Email address should be valid")
        String email,

        @NotBlank(message = "Password shouldn't be blank")
        @Size(min = 8, message = "Password should at least be 8 characters")
        String password,

        @NotBlank(message = "Password shouldn't be blank")
        @Size(min = 8, message = "Password should at least be 8 characters" )
        String confirmPassword
        ) {
}
