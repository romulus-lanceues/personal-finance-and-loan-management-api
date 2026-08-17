package com.lancea.personal_finance_loan_api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lancea.personal_finance_loan_api.entity.Loan;
import com.lancea.personal_finance_loan_api.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record LoanResponse (
        UUID loanId,
        String loanName,
        UUID accountId,
        BigDecimal principal,
        BigDecimal annualRate,
        BigDecimal monthlyPayment,
        int termMonths,
        LoanStatus status,
        LocalDate disbursedAt,
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
    public UUID getResultObjectId() {
        return this.loanId;
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
