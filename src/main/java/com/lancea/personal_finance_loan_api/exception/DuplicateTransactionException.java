package com.lancea.personal_finance_loan_api.exception;

import com.lancea.personal_finance_loan_api.dto.response.TransactionResponse;

public class DuplicateTransactionException extends RuntimeException {
    public DuplicateTransactionException(String message) {
        super(message);
    }
}
