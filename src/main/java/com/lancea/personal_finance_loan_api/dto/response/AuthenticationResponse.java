package com.lancea.personal_finance_loan_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record AuthenticationResponse(
        @Schema(description = "The user ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Stores authentication status message", example = "Success")
        String message,

        @Schema(description = "The JWT token generated that the user can use")
        String token
) {
}
