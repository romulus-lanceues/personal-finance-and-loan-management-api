package com.lancea.personal_finance_loan_api.dto.response;

import java.util.Map;
import java.util.UUID;

public interface AuditableInterface {

    public UUID getResultEntityId();

    public Map<String, Object> generateAuditDetails();
}
