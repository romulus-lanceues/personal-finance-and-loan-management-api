package com.lancea.personal_finance_loan_api.dto.request;


import com.lancea.personal_finance_loan_api.validation.annotation.PasswordMatches;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

@PasswordMatches(groups = ValidationGroups.Register.class)
public record AuthenticationRequest(

        @NotBlank(message = "Name must not be blank", groups = ValidationGroups.Register.class)
        @Size(min = 3, message = "Full name must be 3 characters long", groups = ValidationGroups.Register.class)
        @Null(groups = ValidationGroups.Login.class)
        String fullName,

        @NotBlank(message = "Email shouldn't be blank",
                groups = {ValidationGroups.Register.class, ValidationGroups.Login.class})
        @Email(message = "Email address should be valid",
                groups = {ValidationGroups.Register.class, ValidationGroups.Login.class})
        String email,

        @NotBlank(message = "Password shouldn't be blank",
                groups = {ValidationGroups.Register.class, ValidationGroups.Login.class})
        @Size(min = 8, message = "Password should at least be 8 characters",
                groups = {ValidationGroups.Register.class, ValidationGroups.Login.class})
        String password,

        @NotBlank(message = "Password shouldn't be blank", groups = ValidationGroups.Register.class)
        @Size(min = 8, message = "Password should at least be 8 characters", groups = ValidationGroups.Register.class)
        @Null(groups = ValidationGroups.Login.class)
        String confirmPassword

) {
}
