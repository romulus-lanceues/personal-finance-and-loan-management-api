package com.lancea.personal_finance_loan_api.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record LoanSimulationResponse(
        @Schema(description = "The list of the simulated loan schedules after the additional payment")
        List<LoanScheduleResponse> simulatedSchedule,

        @Schema(description = "The interest saved by the user", example = "100")
        BigDecimal interestSaved,

        @Schema(description = "The months saved by the user", example = "3")
        int monthsSaved
) {
}
