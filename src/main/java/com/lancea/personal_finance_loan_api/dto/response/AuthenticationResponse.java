package com.lancea.personal_finance_loan_api.dto.response;

import java.util.UUID;

public record AuthenticationResponse(
        UUID id,
        String message,
        String token
) {
}
