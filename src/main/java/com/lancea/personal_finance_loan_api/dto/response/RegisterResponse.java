package com.lancea.personal_finance_loan_api.dto.response;

import java.util.UUID;

public record RegisterResponse(UUID uuid,
                               String message) {
}
