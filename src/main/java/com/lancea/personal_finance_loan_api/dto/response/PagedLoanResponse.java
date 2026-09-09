package com.lancea.personal_finance_loan_api.dto.response;

import com.lancea.personal_finance_loan_api.entity.Loan;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

public record PagedLoanResponse(
        @Schema(description = "List of loans")
        List<LoanResponse> content,

        @Schema(description = "Total amount of loans in the database", example = "2000")
        Long totalElements,

        @Schema(description = "The total number of pages for all of the available loan in the database", example = "200")
        Integer totalPages,

        @Schema(description = "The page number of the current page", example = "1")
        Integer number,

        @Schema(description = "The size of content of loans for the current page umber", example = "10")
        Integer size,

        @Schema(description = "Boolean value that indicates if it's the first page of Loans from database", example = "false")
        boolean first,

        @Schema(description = "Boolean value that indicates if it's the last page of Loans from database", example = "true")
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
