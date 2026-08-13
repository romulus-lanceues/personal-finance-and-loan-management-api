package com.lancea.personal_finance_loan_api.dto.response;

import com.lancea.personal_finance_loan_api.entity.Loan;
import org.springframework.data.domain.Page;

import java.util.List;

public record PagedLoanResponse(
        List<LoanResponse> content,
        Long totalElements,
        Integer totalPages,
        Integer number,
        Integer size,
        boolean first,
        boolean last
) {

    public static PagedLoanResponse of(Page<Loan> loans){

        List<LoanResponse> loanResponses = loans.getContent().stream()
                .map(LoanResponse::of)
                .toList();
        return new PagedLoanResponse(
                loanResponses,
                loans.getTotalElements(),
                loans.getTotalPages(),
                loans.getNumber(),
                loans.getSize(),
                loans.isFirst(),
                loans.isLast()
        );
    }
}
