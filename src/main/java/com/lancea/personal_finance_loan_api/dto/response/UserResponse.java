package com.lancea.personal_finance_loan_api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lancea.personal_finance_loan_api.entity.User;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        boolean isDeleted,
        Instant updatedAt
) implements AuditableInterface{

    public static UserResponse of(User user){
        return new UserResponse(user.getId(), user.getEmail(),
                                user.getFullName(), user.getIsDeleted(), user.getUpdatedAt());
    }

    @JsonIgnore
    @Override
    public UUID getResultObjectId() {
        return id;
    }

    @Override
    public Map<String, Object> generateAuditDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("userId", this.id);
        details.put("fullName", this.fullName);
        details.put("email", this.email);
        details.put("isDeleted", this.isDeleted);

        return details;
    }
}
