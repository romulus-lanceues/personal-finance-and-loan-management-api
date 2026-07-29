package com.lancea.personal_finance_loan_api.dto.response;

import com.lancea.personal_finance_loan_api.entity.Transaction;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

public record PagedTransactionResponse(
        List<TransactionResponse> content,
        Long totalElements,
        Integer totalPages,
        Integer number,
        Integer size,
        boolean first,
        boolean last
) {

    public static PagedTransactionResponse of(Page<Transaction> transactions){

        List<TransactionResponse> transactionResponses = transactions.getContent().stream().map(TransactionResponse::of).toList();

        return new PagedTransactionResponse(transactionResponses,
                transactions.getTotalElements(),
                transactions.getTotalPages(),
                transactions.getNumber(),
                transactions.getSize(),
                transactions.isFirst(),
                transactions.isLast());
    }
}
