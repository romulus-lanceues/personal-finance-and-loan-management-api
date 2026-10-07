package com.lancea.personal_finance_loan_api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lancea.personal_finance_loan_api.entity.Loan;
import com.lancea.personal_finance_loan_api.enums.LoanStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record LoanResponse (
        @Schema(description = "Loan ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID loanId,

        @Schema(description = "Loan name", example = "House Loan")
        String loanName,

        @Schema(description = "Account ID", example = "2299b68a-4d2c-471e-b97e-380cca27979f")
        UUID accountId,

        @Schema(description = "Principal amount", example = "10000")
        BigDecimal principal,

        @Schema(description = "Loan annual rate", example = "4.0")
        BigDecimal annualRate,

        @Schema(description = "Loan monthly payment", example = "300")
        BigDecimal monthlyPayment,

        @Schema(description = "Number of months the user must pay the loan", example = "36")
        int termMonths,

        @Schema(description = "The status of the user's loan", example = "ACTIVE" )
        LoanStatus status,

        @Schema(description = "The date when the loan was created", example = "2026-09-02")
        LocalDate disbursedAt,

        @Schema(description = "The date the loan will be fully paid off", example = "2028-09-02")
        LocalDate maturityDate
) implements AuditableInterface {

    public static LoanResponse of(Loan loan){
        return new LoanResponse(
                loan.getId(),
                loan.getLoanName(),
                loan.getAccount().getId(),
                loan.getPrincipal(),
                loan.getAnnualRate(),
                loan.getMonthlyPayment(),
                loan.getTermMonths(),
                loan.getStatus(),
                loan.getDisbursedAt(),
                loan.getMaturityDate());
    }

    @JsonIgnore
    @Override
    public UUID getResultEntityId() {
        return this.accountId;
    }

    @Override
    public Map<String, Object> generateAuditDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("loanId", this.loanId);
        details.put("loanName", this.loanName);
        details.put("accountId", this.accountId);
        details.put("principal", this.principal);
        details.put("annualRate", this.annualRate);
        details.put("termMonths", this.termMonths);
        details.put("monthlyPayment", this.monthlyPayment);
        return details;
    }
}
