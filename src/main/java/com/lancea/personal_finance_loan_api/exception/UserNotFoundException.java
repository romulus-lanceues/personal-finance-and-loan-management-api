package com.lancea.personal_finance_loan_api.exception;


import java.util.UUID;

public class UserNotFoundException extends RuntimeException{

    public UserNotFoundException(UUID userId){
        super("User not found: " + userId);
    }
}
