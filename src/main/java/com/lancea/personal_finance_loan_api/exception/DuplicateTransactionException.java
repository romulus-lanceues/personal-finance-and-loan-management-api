package com.lancea.personal_finance_loan_api.exception;

import com.lancea.personal_finance_loan_api.dto.response.TransactionResponse;

public class DuplicateTransactionException extends RuntimeException {

    private TransactionResponse existingTransaction;

    public DuplicateTransactionException(String message, TransactionResponse existingTransaction) {
        super(message);
        this.existingTransaction = existingTransaction;
    }

    public TransactionResponse getExistingTransaction(){
        return existingTransaction;
    }
}
