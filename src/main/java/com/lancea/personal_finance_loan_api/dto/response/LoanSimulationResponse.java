package com.lancea.personal_finance_loan_api.dto.response;


import java.math.BigDecimal;
import java.util.List;

public record LoanSimulationResponse(
        List<LoanScheduleResponse> simulatedSchedule,
        BigDecimal interestSaved,
        int monthsSaved
) {
}
