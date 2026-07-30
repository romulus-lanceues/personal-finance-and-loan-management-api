package com.lancea.personal_finance_loan_api.repository.projection;

import java.math.BigDecimal;

public interface MonthlySummaryProjection {
    String getCategory();
    BigDecimal getTotalAmount();
    Long getTransactionCount();

}
