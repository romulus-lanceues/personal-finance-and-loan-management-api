package com.lancea.personal_finance_loan_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateInfoRequest(
        String fullName,
        String email) {
}
