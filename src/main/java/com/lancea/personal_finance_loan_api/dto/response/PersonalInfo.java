package com.lancea.personal_finance_loan_api.dto.response;

import java.util.UUID;

public record PersonalInfo(String fullName,
                           String email,
                           UUID userId) {
}
